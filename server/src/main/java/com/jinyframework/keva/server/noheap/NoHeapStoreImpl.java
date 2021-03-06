package com.jinyframework.keva.server.noheap;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

@Slf4j
public class NoHeapStoreImpl implements NoHeapStore {
    protected static final int MEGABYTE = 1024 * 1024;
    protected static final int JOURNAL_SIZE_FACTOR = 100;
    protected static final int DEFAULT_JOURNAL_SIZE = MEGABYTE * JOURNAL_SIZE_FACTOR;

    public static String JOURNAL_VERSION = "JAVAOFFHEAPVERSION_1";
    // Keep an index of LinkedList, where the index key is the size
    // of the empty record. The LinkedList contains pointers (offsets) to
    // the empty records
    //
    public TreeMap<Integer, LinkedList<Long>> emptyIdx = new TreeMap<>();
    // The journal is where persisted data is stored.
    // Intend to move the bulk of the internals out of this class
    // and hide implementation within its own class
    //
    protected RandomAccessFile journal = null;
    protected FileChannel channel = null;
    protected ByteBuffer buffer = null;
    protected int bufferSize = DEFAULT_JOURNAL_SIZE;
    protected int recordCount;
    protected long currentEnd = 0;
    // Keep an index of all active entries in the storage file
    //
    protected IndexStore index = null;
    protected String journalFolder = "";
    protected String journalName = "";
    protected boolean inMemory = true;
    protected boolean reuseExisting = true;
    // Used when iterating through the index
    protected long iterateNext = 0;

    boolean debugLogging = true;
    private long objectGetTime = 0;
    private long objectPutTime = 0;

    private NoHeapStoreImpl() {
    }

    // TODO: Need to add optional expected capacity
    public NoHeapStoreImpl(String folder, String name) {
        this(folder, name, Storage.IN_MEMORY, DEFAULT_JOURNAL_SIZE, true);
    }

    public NoHeapStoreImpl(String folder, String name, Storage type) {
        this(folder, name, type, DEFAULT_JOURNAL_SIZE, true);
    }

    public NoHeapStoreImpl(String folder, String name, Storage type, int sizeInBytes) {
        this(folder, name, type, sizeInBytes, true);
    }

    public NoHeapStoreImpl(String folder, String name, Storage type,
                           int sizeInBytes, boolean reuseExisting) {
        this.reuseExisting = reuseExisting;
        this.journalFolder = folder;
        this.journalName = name;
        this.inMemory = (type == Storage.IN_MEMORY);
        this.bufferSize = sizeInBytes;

        String journalPath = createJournalFolderName(journalFolder, journalName);

        createIndexJournal(journalPath, inMemory, reuseExisting);
        createMessageJournal(journalPath, inMemory, reuseExisting);
    }

    protected final void createMessageJournal(String journalPath, boolean inMemory, boolean reuseExisting) {
        if (inMemory) {
            // In-memory ByteBuffer Journal
            createMessageJournalBB();
        } else {
            // Persisted File MappedByteBuffer Journal
            createMessageJournalMBB(journalPath, reuseExisting);
        }
    }

    protected final void createMessageJournalBB() {
        try {
            buffer = ByteBuffer.allocateDirect(bufferSize);
            currentEnd = buffer.position();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    protected final String createJournalFolderName(String folder, String name) {
        return journalFolder + File.separator +
                journalName + "Data";
    }

    protected final void createMessageJournalMBB(String journalPath, boolean reuseExisting) {
        try {
            // First create the directory
            File filePath = new File(journalFolder);
            boolean created = filePath.mkdir();
            if (!created) {
                // It may have failed because the directory already existed
                if (!filePath.exists()) {
                    log.error("Directory creation failed: " + journalFolder);
                    return;
                }
            }

            // If the journal file already exists, rename it unless we're
            // supposed to reuse the existing file and its contents
            boolean fileExists = false;
            try {
                File file = new File(journalPath);
                fileExists = file.exists();
                if (fileExists && !reuseExisting) {
                    File newFile = new File(journalPath + "_prev");
                    log.info("Moving journal " + journalPath + " to " + newFile.getName());
                    file.renameTo(newFile);
                }
            } catch (Exception ignored) {
            }

            journal = new RandomAccessFile(journalPath, "rw");
            if (fileExists && reuseExisting) {
                // Existing file, so use its existing length
                bufferSize = (int) journal.length();
            } else {
                // New file, set its length
                journal.setLength(bufferSize);
            }

            channel = journal.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);

            if (fileExists && reuseExisting) {
                // Iterate through the existing records and find its current end
                currentEnd = scanJournal();

                if (debugLogging) {
                    log.info("Initialized journal '" + journalName +
                            "', existing filename=" + journalPath);
                }
            } else {
                //
                // Write some journal header data
                //
                writeJournalHeader(journal);

                currentEnd = journal.getFilePointer();

                if (debugLogging) {
                    log.info("Created journal '" + journalName +
                            "', filename=" + journalPath);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    //
    // Iterate through the contents of the journal and create
    // an index for the empty records
    //
    private long scanJournal() {
        for (LinkedList<Long> val : emptyIdx.values()) {
            val.clear();
        }
        emptyIdx.clear();

        int recordSize = 0;

        try {
            long fileSize = journal.length();
            String version = journal.readUTF();
            String name = journal.readUTF();
            Long createTime = journal.readLong();
            currentEnd = journal.getFilePointer();
            ByteBuffer bb = buffer;
            bb.position((int) currentEnd);

            // Iterate through the records in the file with the intent to
            // record the empty slots (for future reuse) and the end of the
            // saved record
            //
            while (currentEnd < (fileSize - Header.HEADER_SIZE)) {
                // Begin reading the next record header
                //

                // Active Record?
                boolean active = true;
                if (bb.get() == INACTIVE_RECORD) {
                    active = false;
                }

                // Read record type
                byte type = bb.get();
                if (type == 0) {
                    bb.position((int) currentEnd);
                    break; // end of data records in file
                }

                // Get the data length (comes after the record header)
                int dataLength = bb.getInt();
                recordSize = Header.HEADER_SIZE + dataLength;

                if (!active) {
                    // Record the inactive record location for reuse
                    storeEmptyRecord(currentEnd, dataLength);
                }

                // skip past the data to the beginning of the next record
                currentEnd += recordSize;
                bb.position((int) currentEnd);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                String sb = "Persist data: " + " Journal: " + journalName +
                        ", length: " + channel.size() +
                        ", currentEnd: " + currentEnd +
                        ", recordSize: " + recordSize;
                log.info(sb);
            } catch (Exception ignored) {
            }
        }

        return currentEnd;
    }

    private void writeJournalHeader(RandomAccessFile journal) throws IOException {
        // write the journal version number to the file
        journal.writeUTF(NoHeapStoreImpl.JOURNAL_VERSION);

        // write the journal name to the file
        journal.writeUTF(journalName);

        // identify the journal as belonging to this server's run
        // (to avoid it from being recovered by the recovery thread)
        journal.writeLong(System.currentTimeMillis());
    }

    protected void createIndexJournal(String journalPath, boolean inMemory, boolean reuseExisting) {
        try {
            int size = bufferSize / 4;
            index = new IndexStoreImpl(size, journalPath, inMemory, reuseExisting);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private JournalLocationData getStorageLocation(int recordLength) {
        JournalLocationData location = new JournalLocationData();
        location.offset = -1;   // where to write new record
        location.newEmptyRecordSize = -1; // Left over portion of empty space

        // Check if the deleted record list is empty
        if (emptyIdx == null || emptyIdx.isEmpty()) {
            return location;
        }

        try {
            // Determine if there's an empty location to insert this new record
            // There are a few criteria. The empty location must either be
            // an exact fit for the new record with its header (replacing the
            // existing empty record's header, or if the empty record is larger,
            // it must be large enough for the new record's header and data,
            // and another header to mark the empty record so the file can
            // be traversed at a later time. In other words, the journal
            // consists of sequential records, back-to-back, with no gap in
            // between, otherwise it cannot be traversed from front to back
            // without adding a substantial amount of indexing within the file.
            // Therefore, even deleted records still exist within the journal,
            // they are simply marked as deleted. But the record size is still
            // part of the record so it can be skipped over when read back in

            // First locate an appropriate location. It must match exactly
            // or be equal in size to the record to write and a minimal record
            // which is just a header (5 bytes). So, HEADER + DATA + HEADER,
            // or (data length + 10 bytes).

            // Is there an exact match?
            LinkedList<Long> records = emptyIdx.get(recordLength);
            if (records != null && !records.isEmpty()) {
                location.offset = records.remove();

                // No need to append an empty record, just return offset
                return location;
            }

            // Can't modify the empty record list while iterating so
            // create a list of objects to remove (they are actually entries
            // of a size value)
            //
            ArrayList<Integer> toRemove = new ArrayList<>();

            // No exact size match, find one just large enough
            for (Integer size : this.emptyIdx.keySet()) {
                // If we're to split this record, make sure there's enough
                // room for the new record and another empty record with
                // a header and at least one byte of data
                //
                if (size >= recordLength + Header.HEADER_SIZE + 1) {
                    records = emptyIdx.get(size);
                    if (records == null || records.size() == 0) {
                        // This was the last empty record of this size
                        // so delete the entry in the index and continue
                        // searching for a larger empty region (if any)
                        toRemove.add(size);
                        continue;
                    }

                    location.offset = records.remove();

                    // We need to append an empty record after the new record
                    // taking the size of the header into account
                    location.newEmptyRecordSize =
                            (size - recordLength - Header.HEADER_SIZE);

                    int newOffset =
                            (int) location.offset + recordLength + Header.HEADER_SIZE;

                    // Store the new empty record's offset
                    storeEmptyRecord(newOffset, location.newEmptyRecordSize);
                    break;
                }
            }

            // Remove any index records marked to delete
            //
            for (Integer offset : toRemove) {
                emptyIdx.remove(offset);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return location;
    }

    private JournalLocationData setNewRecordLocation(int dataLength) {
        int recordSize = Header.HEADER_SIZE + dataLength;

        try {
            // Always persist messages at the end of the journal unless
            // there's an empty position large enough within the journal
            //
            JournalLocationData location = getStorageLocation(dataLength);
            if (location.offset == -1) {
                // None found, need to add record to the end of the journal
                // Seek there now only if we're not already there
                long currentPos = buffer.position();
                if (currentPos != currentEnd) {
                    currentPos = buffer.position((int) currentEnd).position();
                }

                // Check to see if we need to grow the journal file

                long journalLen;
                if (!inMemory) {
                    journalLen = channel.size();
                } else {
                    journalLen = buffer.capacity();
                }

                if ((currentPos + recordSize) >= journalLen) {
                    // Need to grow the buffer/file by another page
                    currentPos = expandJournal(journalLen);
                }

                location.offset = currentEnd;

                // Increment currentEnd by the size of the record appended
                currentEnd += recordSize;
            } else {
                // Seek to the returned insertion point
                buffer.position((int) location.offset);
            }

            return location;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(-1);
        }

        return null;
    }

    protected long expandJournal(long journalLen) throws IOException {
        if (debugLogging) {
            log.info("Expanding journal size");
        }

        if (inMemory) {
            long newLength =
                    journalLen + (MEGABYTE * JOURNAL_SIZE_FACTOR);
            log.info("Expanding ByteBuffer size to " + newLength + "...");
            ByteBuffer newBuffer = ByteBuffer.allocateDirect((int) newLength);
            if (buffer.hasArray()) {
                byte[] array = buffer.array();
                newBuffer.put(array);
            } else {
                buffer.position(0);
                newBuffer.put(buffer);
            }
            buffer = newBuffer;
        } else {
            log.info("Expanding RandomAccessFile journal size to " + (journalLen + (MEGABYTE * JOURNAL_SIZE_FACTOR)) + "...");
            ((MappedByteBuffer) buffer).force();
            journal.setLength(journalLen + (MEGABYTE * JOURNAL_SIZE_FACTOR));
            channel = journal.getChannel();
            journalLen = channel.size();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, journalLen);
        }
        log.info("Expanded journal size");

        // Since we re-mapped the file, double-check the position
        long currentPos = buffer.position();
        if (currentPos != currentEnd) {
            buffer.position((int) currentEnd);
        }

        return currentPos;
    }

    protected void storeEmptyRecord(long offset, int length) {
        // Store the empty record in an index. Look to see if there
        // are other records of the same size (in a LinkedList). If
        // so, add this one to the end of the linked list
        //
        LinkedList<Long> emptyRecs = emptyIdx.computeIfAbsent(length, k -> new LinkedList<>());
        // There are no other records of this size. Add an entry
        // in the hash table for this new linked list of records

        // Add the pointer (file offset) to the new empty record
        emptyRecs.add(offset);
    }

    public int getIndexLoad() {
        return this.index.getLoad();
    }

    // Empties the journal file and resets indexes
    @Override
    public void delete() {
        try {
            // Clear the existing indexes first
            //
            index.reset();
            emptyIdx.clear();

            if (inMemory) {
                buffer.clear();
                buffer.limit(0);
                buffer = ByteBuffer.allocateDirect(0);
            } else {
                // Reset the file pointer and length
                journal.seek(0);
                channel.truncate(0);
                channel.close();
                journal.close();
                File f = new File(createJournalFolderName(journalFolder, journalName));
                f.delete();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized long getRecordCount() {
        return recordCount;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Persistence interface
    //

    @Override
    public synchronized long getEmptyCount() {
        return emptyIdx.size();
    }

    @Override
    public String getName() {
        return journalName;
    }

    @Override
    public String getFolder() {
        return journalFolder;
    }

    @Override
    public synchronized long getFileSize() {
        try {
            return channel.size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean putLong(String key, Long val) {
        return putVal(key, val, LONG_RECORD_TYPE);
    }

    @Override
    public boolean putInteger(String key, Integer val) {
        return putVal(key, val, INT_RECORD_TYPE);
    }

    @Override
    public boolean putShort(String key, Short val) {
        return putVal(key, val, SHORT_RECORD_TYPE);
    }

    @Override
    public boolean putChar(String key, char val) {
        return putVal(key, val, CHAR_RECORD_TYPE);
    }

    @Override
    public boolean putFloat(String key, Float val) {
        return putVal(key, val, FLOAT_RECORD_TYPE);
    }

    @Override
    public boolean putDouble(String key, Double val) {
        return putVal(key, val, DOUBLE_RECORD_TYPE);
    }

    @Override
    public boolean putString(String key, String val) {
        return putVal(key, val, TEXT_RECORD_TYPE);
    }

    @Override
    public boolean putObject(String key, Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            // Grab the payload and determine the record size
            //
            objectOutputStream.writeObject(obj);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            objectOutputStream.close();

            return putVal(key, bytes, BYTEARRAY_RECORD_TYPE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public Long getLong(String key) {
        return (Long) getValue(key, LONG_RECORD_TYPE);
    }

    @Override
    public Integer getInteger(String key) {
        return (Integer) getValue(key, INT_RECORD_TYPE);
    }

    @Override
    public Short getShort(String key) {
        return (Short) getValue(key, SHORT_RECORD_TYPE);
    }

    @Override
    public Float getFloat(String key) {
        return (Float) getValue(key, FLOAT_RECORD_TYPE);
    }

    @Override
    public Double getDouble(String key) {
        return (Double) getValue(key, DOUBLE_RECORD_TYPE);
    }

    @Override
    public char getChar(String key) {
        Object obj = getValue(key, CHAR_RECORD_TYPE);
        if (obj != null) {
            return (char) obj;
        }
        return (char) 0;
    }

    @Override
    public String getString(String key) {
        return (String) getValue(key, TEXT_RECORD_TYPE);
    }

    @Override
    public Object getObject(String key) {
        Object object = null;

        Object obj = this.getValue(key, BYTEARRAY_RECORD_TYPE);
        if (obj == null) {
            return null;
        }

        byte[] bytes = (byte[]) obj;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(bis)) {
            object = objectInputStream.readObject();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return object;
    }

    @Override
    public boolean remove(String key) {
        Long offset = (long) -1;
        int dataLength = -1;

        try {
            synchronized (this) {
                // Locate the message in the journal
                offset = getRecordOffset(key);

                if (offset == -1) {
                    return false;
                }

                // read the header (to get record length) then set it as inactive
                buffer.position(offset.intValue());
                buffer.put(INACTIVE_RECORD);
                buffer.put(EMPTY_RECORD_TYPE);
                dataLength = buffer.getInt();

                // Store the empty record location and size for later reuse
                storeEmptyRecord(offset, dataLength);

                // Remove from the journal index
                index.remove(key);
            }

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            log.error("deleteMessage data, offset=" + offset + ", length=" + dataLength + ", bufferSize=" + bufferSize);
            try {
                log.error("current journal data, filePointer=" + journal.getFilePointer()
                        + ", filesize=" + journal.length());
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    protected boolean putVal(String key, Object val, byte type) {
        // Each message is written to a file with the following
        // record structure:
        //
        // HEADER:
        //   Boolean    - Active record indicator
        //   Byte       - Message type (0=Empty, 1=Bytes, 2=String)
        //   Integer    - Size of record's payload (not header)
        //
        // DATA:
        //   Byte array or data value - The message payload
        //
        try {
            long start = System.currentTimeMillis();
            synchronized (this) {
                int dataLength;

                switch (type) {
                    case LONG_RECORD_TYPE:
                        dataLength = Long.BYTES;
                        break;
                    case INT_RECORD_TYPE:
                        dataLength = Integer.BYTES;
                        break;
                    case DOUBLE_RECORD_TYPE:
                        dataLength = Double.BYTES;
                        break;
                    case FLOAT_RECORD_TYPE:
                        dataLength = Float.BYTES;
                        break;
                    case SHORT_RECORD_TYPE:
                        dataLength = Short.BYTES;
                        break;
                    case CHAR_RECORD_TYPE:
                        dataLength = 2; // 16-bit Unicode character
                        break;
                    case TEXT_RECORD_TYPE:
                        dataLength = ((String) val).getBytes().length;
                        break;
                    case BYTEARRAY_RECORD_TYPE:
                        dataLength = ((byte[]) val).length;
                        break;
                    default:
                        return false;
                }

                JournalLocationData location = setNewRecordLocation(dataLength);

                // First write the header
                //
                buffer.put(ACTIVE_RECORD);
                buffer.put(type);
                buffer.putInt(dataLength);

                // Write record value
                //
                switch (type) {
                    case LONG_RECORD_TYPE:
                        buffer.putLong((Long) val);
                        break;
                    case INT_RECORD_TYPE:
                        buffer.putInt((Integer) val);
                        break;
                    case DOUBLE_RECORD_TYPE:
                        buffer.putDouble((Double) val);
                        break;
                    case FLOAT_RECORD_TYPE:
                        buffer.putFloat((Float) val);
                        break;
                    case SHORT_RECORD_TYPE:
                        buffer.putShort((Short) val);
                        break;
                    case CHAR_RECORD_TYPE:
                        buffer.putChar((char) val);
                        break;
                    case TEXT_RECORD_TYPE:
                        buffer.put(((String) val).getBytes());
                        break;
                    case BYTEARRAY_RECORD_TYPE:
                        buffer.put((byte[]) val);
                        break;
                }

                // Next, see if we need to append an empty record if we inserted
                // this new record at an empty location
                if (location.newEmptyRecordSize != -1) {
                    // Write the header and data for the new record, as well
                    // as header indicating an empty record
                    buffer.put(INACTIVE_RECORD); // inactive record
                    buffer.put(EMPTY_RECORD_TYPE); // save message type EMPTY
                    buffer.putInt(location.newEmptyRecordSize);

                    if (buffer.position() > currentEnd) {
                        currentEnd = buffer.position();
                    }
                }

                indexRecord(key, location.offset);

                recordCount++;

                long end = System.currentTimeMillis();
                this.objectPutTime += (end - start);

                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    protected Object getValue(String key, byte type) {
        Long offset = getRecordOffset(key);
        if (offset != null && offset > -1) {
            return getValue(offset, type);
        }
        return null;
    }

    protected Object getValue(Long offset, byte type) {
        Object val = null;
        try {
            if (offset != null && offset > -1) {
                long start = System.currentTimeMillis();

                // Jump to this record's offset within the journal file
                buffer.position(offset.intValue());

                // First, read in the header
                byte active = buffer.get();
                if (active != 1) {
                    return null;
                }

                byte typeStored = buffer.get();
                if (type != typeStored) {
                    return null;
                }

                int dataLength = buffer.getInt();

                byte[] bytes;
                switch (type) {
                    case LONG_RECORD_TYPE:
                        val = buffer.getLong();
                        break;
                    case INT_RECORD_TYPE:
                        val = buffer.getInt();
                        break;
                    case DOUBLE_RECORD_TYPE:
                        val = buffer.getDouble();
                        break;
                    case FLOAT_RECORD_TYPE:
                        val = buffer.getFloat();
                        break;
                    case SHORT_RECORD_TYPE:
                        val = buffer.getShort();
                        break;
                    case CHAR_RECORD_TYPE:
                        val = buffer.getChar();
                        break;
                    case BYTEARRAY_RECORD_TYPE:
                        bytes = new byte[dataLength];
                        buffer.get(bytes);
                        val = bytes;
                        break;
                    case TEXT_RECORD_TYPE:
                        bytes = new byte[dataLength];
                        buffer.get(bytes);
                        val = new String(bytes);
                        break;
                }

                long end = System.currentTimeMillis();
                this.objectGetTime += (end - start);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return val;
    }

    @Override
    public Object iterateStart() {
        try {
            long current = 0;

            // Get past all the file header data
            //
            if (journal != null) {
                journal.seek(0);
                long filesize = journal.length();
                String version = journal.readUTF();
                String name = journal.readUTF();
                Long createTime = journal.readLong();
                current = journal.getFilePointer();
            }

            // Return the first active record found
            return getNextRecord(current);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Object iterateNext() {
        return getNextRecord(iterateNext);
    }

    protected Object getNextRecord(long current) {
        int recordSize;
        try {
            ByteBuffer bb = buffer;
            if (bb.position() != current) {
                bb.position((int) current);
            }

            // Iterate through the records in the file with the intent to
            // record the empty slots (for future reuse) and the end of the
            // saved record
            //
            boolean found = false;
            byte type = NoHeapStore.EMPTY_RECORD_TYPE;
            while (!found && current < (bufferSize - Header.HEADER_SIZE)) {
                // Begin reading the next record header
                //

                // Active Record?
                boolean active = true;
                if (bb.get() == INACTIVE_RECORD) {
                    active = false;
                }

                // Read record type
                type = bb.get();
                if (type == 0) {
                    bb.position((int) currentEnd);
                    break; // end of data records in file
                }

                // Get the data length (comes after the record header)
                int datalen = bb.getInt();
                recordSize = Header.HEADER_SIZE + datalen;

                if (active) {
                    // Found the next active record
                    found = true;

                    // Store the location to the start of the next record
                    iterateNext = current + recordSize;
                } else {
                    // skip past the data to the beginning of the next record
                    current += recordSize;
                    bb.position((int) current);
                }
            }

            if (found) {
                // Return the record
                return getValue(current, type);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    protected Long getRecordOffset(String key) {
        return index.get(key);
    }

    protected void indexRecord(String key, long recordLocation) {
        index.put(key, recordLocation);
    }

    public int getCollisions() {
        return index.getCollisions();
    }

    public long getObjectRetrievalTime() {
        return objectGetTime;
    }

    public long getObjectStorageTime() {
        return objectPutTime;
    }

    public void outputStats() {
        log.info("Data Store:");
        log.info(" -size: " + buffer.capacity());
        index.outputStats();
    }

    public static class Header implements Serializable {
        //   Boolean    - Active record indicator
        //   Byte       - Message type (0=Empty, 1=Bytes, 2=String)
        //   Integer    - Size of record's payload (not header)
        public static final int HEADER_SIZE = Integer.BYTES + 2;
        byte active;            // 1 byte
        byte type;              // 1 byte
        int size;               // 4 bytes
    }

    static class JournalLocationData {
        long offset;
        int newEmptyRecordSize;
    }
}
