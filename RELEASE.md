# kotlinx-datetime release checklist

## Full release

To release `kotlinx-datetime`:

1. Make sure there are no updates to either Windows/IANA timezone name mapping
   or to the IANA timezone database that we publish.
   * <https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxDatetime_Check_Updates>
     checks this automatically every night.
   * Follow the procedure in
     [UPDATE_TIMEZONE_DATABASE.md](UPDATE_TIMEZONE_DATABASE.md)
     if there are updates or to check for updates manually.

2. Check out the `master` branch:
   > `git checkout master`

3. Retrieve the most recent version of it:
   > `git pull`

4. Search & replace `<old-version>` with `<version>` in
   * Documentation: [README.md](README.md)
   * [gradle.properties](gradle.properties)
   * **NOT** [CHANGES.md](CHANGES.md)!

5. Write release notes in [CHANGES.md](CHANGES.md):
   * Follow the established style from the earlier release notes.
   * Write each change on a single line (don't wrap with a caret return).
   * Look through the commit messages since the previous release:
     > `git log v<old-version>..`

     Example: `git log v0.7.0..` lists the commits made since 0.7.0.

6. Create a new branch for this release:
   > `git checkout -b version-<version>`

7. Commit and push the changes:
   > `git commit -a -m 'Version <version>'`
   > `git push -u origin version-<version>`

8. Open a GitHub pull request and review it.
   Wait for the CI to finish.

### Publishing a normal release with the compatibility artifact

For the nearest future, **follow this subsection**.
When we no longer have the compatibility artifact, we'll remove the subsection.

9. Create a new branch from `version-<version>`:
   > `git checkout -b version-<version>-compat version-<version>`

10. Replace `<version>` with `<version>-0.6.x-compat`
   in [gradle.properties](gradle.properties).

11. Commit and push the changes:
   > `git commit -a -m 'Version <version>, compatibility artifact`
   > `git push -u origin version-<version>-compat`

12. Create another branch from `version-<version>`:
   > `git checkout -b version-<version>-normal version-<version>`

13. Merge the `dkhalanskyjb/remove-deprecated-instant` branch:
   > `git merge dkhalanskyjb/remove-deprecated-instant`

14. Push the changes:
   > `git push -u origin version-<version>-normal`

15. Double-check the results.
   The `normal` branch should be different from `compat` in having removed
   a lot of code related to `kotlinx.datetime.Instant` and
   `kotlinx.datetime.Clock` and having a `version-<version>` in
   `gradle.properties` instead of `version-<version>-0.6.x-compat`.
   > `git diff version-<version>-compat version-<version>-normal`

16. In TeamCity, start deployment of `version-<version>-compat` by running the
   `Deployment/Start Deployment [RUN THIS ONE]` configuration:
   <https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxDatetime_StartDeployment>.
   Use the `Run custom build` button.
   - In the `Changes` tab, select the build branch `version-<version>-compat`.
   - In the `Parameters` tab, set the parameters:
     * `Version` to `<version>-0.6.x-compat`.
     * Leave `VersionSuffix` blank.
     * Leave `ZoneInfoVersion` blank.

17. Start deployment of `version-<version>-normal`.
   - In the `Changes` tab, select the build branch `version-<version>-normal`.
   - In the `Parameters` tab, set the parameters:
       * `Version` to `<version>`.
       * Leave `VersionSuffix` blank.
       * Leave `ZoneInfoVersion` blank.

18. Wait for the *eight* deployment tasks to finish:
   for both `version-<version>-normal` and `version-<version>-compat`,
   `Start Deployment [RUN THIS ONE]`, `DeployCentral (Mac OS X)`,
   `Deploy To Central`, and `Deploy ZoneInfo To Central` need to succeed.

19. Notify the release facilitator.
   Publish the release cooperatively.
   The artifacts to publish are:
   - `kotlinx-datetime` version `<version>`.
   - `kotlinx-datetime` version `<version>-0.6.x-compat`.
   - `kotlinx-datetime-zoneinfo` version `<tzdb_tag>-spi.<version>`.
     *NOT* `<tzdb_tag>-spi.<version>-0.6.x-compat`!

20. Merge `version-<version>` into `master`:
   > `git checkout master`
   > `git merge version-<version>`
   > `git push`

21. In [GitHub](https://github.com/Kotlin/kotlinx-datetime):
   * Create a release named `v<version>`, creating the `v<version>` tag.
   * Cut & paste lines from [CHANGES.md](CHANGES.md) into the description.

22. Set the `latest-version` branch to `v<version>`.
   > `git checkout latest-release`
   > `git merge --ff-only master`
   > `git push`

23. Announce the new release in [Slack](https://kotlinlang.slack.com).

24. Propose the website documentation update:
   * In the `JetBrains/kotlin-web-site` repository:
       - Update `dateTimeVersion` to `<version>` in
         <https://github.com/JetBrains/kotlin-web-site/blob/master/docs/v.list>.
       - Update `KOTLINX_DATETIME_RELEASE_LABEL` to `v<version>` in
         <https://github.com/JetBrains/kotlin-web-site/blob/master/.teamcity/BuildParams.kt>.
       - Create a pull request with the changes.
   * In the `JetBrains/kotlin-compiler-server` repository:
       - Update `kotlinx-datetime` to `<version>` in
         <https://github.com/JetBrains/kotlin-compiler-server/blob/master/gradle/libs.versions.toml>
       - Create a pull request with the changes.

25. Remove the `version-<version>-normal`, `version-<version>-compat`, and
   `version-<version>` branches.

### Publishing a normal release

9. In TeamCity, start deployment of `version-<version>` by running the
   `Deployment/Start Deployment [RUN THIS ONE]` configuration:
   <https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxDatetime_StartDeployment>.
   Use the `Run custom build` button.
   - In the `Changes` tab, select the build branch `version-<version>`.
   - In the `Parameters` tab, set the parameters:
     * `Version` to `<version>`.
     * Leave `VersionSuffix` blank.
     * Leave `ZoneInfoVersion` blank.

10. Wait for the *four* deployment tasks to finish:
   `Start Deployment [RUN THIS ONE]`, `DeployCentral (Mac OS X)`,
   `Deploy To Central`, and `Deploy ZoneInfo To Central` need to succeed.

11. Notify the release facilitator.
   Publish the release cooperatively.
   The artifacts to publish are:
   - `kotlinx-datetime` version `<version>`.
   - `kotlinx-datetime-zoneinfo` version `<tzdb_tag>-spi.<version>`.

12. Merge `version-<version>` into `master`:
   > `git checkout master`
   > `git merge version-<version>`
   > `git push`

13. In [GitHub](https://github.com/Kotlin/kotlinx-datetime):
   * Create a release named `v<version>`, creating the `v<version>` tag.
   * Cut & paste lines from [CHANGES.md](CHANGES.md) into the description.

14. Set the `latest-version` branch to `v<version>`.
   > `git checkout latest-release`
   > `git merge --ff-only master`
   > `git push`

15. Announce the new release in [Slack](https://kotlinlang.slack.com).

16. Propose the website documentation update:
   * In the `JetBrains/kotlin-web-site` repository:
       - Update `dateTimeVersion` to `<version>` in
         <https://github.com/JetBrains/kotlin-web-site/blob/master/docs/v.list>.
       - Update `KOTLINX_DATETIME_RELEASE_LABEL` to `v<version>` in
         <https://github.com/JetBrains/kotlin-web-site/blob/master/.teamcity/BuildParams.kt>.
       - Create a pull request with the changes.
   * In the `JetBrains/kotlin-compiler-server` repository:
       - Update `kotlinx-datetime` to `<version>` in
         <https://github.com/JetBrains/kotlin-compiler-server/blob/master/gradle/libs.versions.toml>
       - Create a pull request with the changes.

17. Remove the `version-<version>` branch.

## Publishing just the timezone database

> TODO: this section needs to be updated when either Kotlin/Wasm/WASI
reaches stability or we start publishing the timezone database for other
platforms.
Then, it will not be enough to just publish the database for the latest version,
we will need to publish the timezone database for the older `kotlinx-datetime`
versions as well.

1. Update the IANA timezone database as specified in
  [UPDATE_TIMEZONE_DATABASE.md](UPDATE_TIMEZONE_DATABASE.md).
  `tzdb-<tzdb_tag>` should now contain an up-to-date timezone database
  that wasn't yet published.

2. In TeamCity, start deployment of `tzdb-<tzdb_tag>` by running the
   `Deployment/Start Deployment [RUN THIS ONE]` configuration:
   <https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxDatetime_StartDeployment>.
   Use the `Run custom build` button.
   - In the `Changes` tab, select the build branch `tzdb-<tzdb_tag>`.
   - In the `Parameters` tab, set the parameters:
     * `Version` to the latest published `kotlinx-datetime` version.
       Example: `0.7.1`.
     * Leave `VersionSuffix` blank.
     * Leave `ZoneInfoVersion` blank.

3. Wait for the *four* deployment tasks to finish:
   `Start Deployment [RUN THIS ONE]`, `DeployCentral (Mac OS X)`,
   `Deploy To Central`, and `Deploy ZoneInfo To Central` need to succeed.

4. Notify the release facilitator.
   Publish the release cooperatively.
   The artifact to publish is
   `kotlinx-datetime-zoneinfo` version `<tzdb_tag>-spi.<latest_version>`.
