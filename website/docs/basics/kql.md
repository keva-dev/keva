---
sidebar_position: 7
---

# KQL

:::warning WIP
This feature is under development
:::

Naive support to SQL-like queries syntax in KevaDB (exposing via RESP protocol).

![](https://user-images.githubusercontent.com/13906546/142739291-e74173ff-c712-4966-b443-0b7a929a30d2.png)

KQL currently supports (partially SQL syntax):
- CREATE TABLE
- DROP TABLE
- INSERT
- UPDATE
- DELETE
- SELECT
- AND, OR
- EQUAL, NOT EQUAL
- MIN, MAX
- COUNT, AVG, SUM
- NULL VALUES
- GROUP BY
- OFFSET
- LIMIT
