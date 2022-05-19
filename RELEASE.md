# Release flow

Please follow the steps below to release a new version of Keva.

Requirements:

- Unix shell
- GPG key
- Sonatype Nexus credentials (need to request)
- Docker Hub credentials (need to request)

If you do not have these, please email `oss@keva.dev` to request.

## Preparation

Before each release, we can group issues in a [GitHub's Milestone](https://github.com/keva-dev/keva/milestones),
create a GitHub milestone, and assign it to the `release` label, the milestone name should be `vX.Y.Z`.

## Create release branch

First, please create a release branch from `master` branch:

```
git fetch
git checkout master
git pull origin master
### Add a new release branch, e.g. `release/1.1.0`
git checkout -b release/1.1.0
```

Then, please pull (rebasing) the latest changes from `develop` branch or cherry-pick the commits:

```
git fetch
git pull origin develop --rebase
```

Push the release branch to `origin`:

```
git push origin release/1.1.0
```

And create a pull request on Github, request reviewers, after the pull request is merged, you can release the new version.

Create a version tag (via git command or Github UI):

```
git tag -a v1.1.0 -m "Keva v1.0.0-rc0"
```

Remember to add binary artifacts to the release tag (check how to get binary artifacts below).

## Build binary (bash based) artifacts

Run:

```bash
sh build.sh
```

## Publish to Maven Central

Update version at `build.gradle`:

```
group 'dev.keva'
version '1.0.0-rc0'
```

Set environment variables at `./gradlew.properties`:

```
sonatypeUsername=SONARUSERNAME
sonatypePassword=SONARPASSWORD
signing.keyId=KeyID # e.g. "0xAA279C9C"
signing.password=password
signing.secretKeyRingFile=FileLocation # e.g. "/Users/keva/.gnupg/secring.gpg"
```

Run:

```bash
./gradlew publish
```

## Publish to Docker Hub

Login to Docker Hub:

```bash
docker login -u kevadev -p kevapassword
```

Build Docker image:

```bash
docker build -t keva-server .
```

Tag and push image:

```bash
docker tag keva-server:latest kevadev/keva-server:1.0.0-rc0
docker push kevadev/keva-server:1.1.0
```
