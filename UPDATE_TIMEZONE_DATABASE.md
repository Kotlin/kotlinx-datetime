# Updating the timezone database

If the build configuration <https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxDatetime_Check_Updates>
fails, the timezone databases bundled with `kotlinx-datetime` need updating.
This means either the IANA timezone database artifact provided for Wasm/WASI,
the Windows/IANA timezone name mapping, or both.

## Updating the IANA timezone database artifact

We publish a separate artifact for Wasm/WASI that includes
the IANA timezone database, as Wasm/WASI does not provide access to the
timezone database out of the box.
This means that whenever a new version of the timezone database gets published,
we need to release a new version of the artifact.

1. Ensure you have no changes in `timezones/full/tzdb/`:
   > `git status timezones/full/tzdb/`

2. Check out `master`, make sure it's up to date:
   > `git checkout master; git pull`

3. Visit <https://github.com/eggert/tz/tags> and note the latest available tag
   (`<tzdb_tag>`).

4. Search & replace `<old_tzdb_tag>` with `<tzdb_tag>`.
   - In `gradle.properties`, replace `tzdbVersion=<old_tzdb_tag>`
     with `tzdbVersion=<tzdb_tag>`.
   - In `README.md`,
     replace `<old_tzdb_tag>` in the Gradle configuration snippet
     with `<tzdb_tag>`.

5. Call the `:kotlinx-datetime-zoneinfo:tzdbDownloadAndCompile` Gradle task:
   > `./gradlew tzdbDownloadAndCompile`

6. Create a new branch:
   > `git checkout -b tzdb-<tzdb_tag>`

7. Commit and push the changes:
   > `git commit timezones/full/tzdb -m 'Use IANA tzdb <tzdb_tag>'`
   > `git push -u origin tzdb-<tzdb_tag>`

8. Create a GitHub pull request and review the changes.

9. Wait for the CI build to pass.

10. Follow the procedure for publishing a timezone database-only release
   (see [RELEASE.md](RELEASE.md)).

11. Squash-and-merge the branch.

## Updating Windows/IANA timezone name mappings

Windows uses its own convention for timezone names.
We store internally the mapping between IANA timezone names
(which we use for all platforms as a standard) and the
Windows-specific names encountered in the Windows registry.

1. Stash the changes you have:
   > `git stash`

2. Check out `master`, make sure it's up to date:
   > `git checkout master; git pull`

3. Call the `:kotlinx-datetime:downloadWindowsZonesMapping` Gradle task:
   > `./gradlew downloadWindowsZonesMapping`

4. If the task succeeds, the library already has the most up-to-date version
   of the name mapping.
   No actions are necessary.

5. If the task fails, it should say
   `The new mappings were written to the filesystem.`.
   Verify this by checking the difference:
   > `git diff`

6. Create a new branch:
   > `git checkout -b update-windows-tz-names`

7. Commit and push the changes:
   > `git commit -a -m "Update the Windows/IANA timezone name mappings"`
   > `git push -u origin update-windows-tz-names`

8. Create a GitHub pull request and review the changes.
   Normally, the changes are small: only a few timezone names are added,
   or some deprecated timezone names are removed.
   Verify that this is the case.

9. Wait for the CI build to pass.

10. Squash-and-merge the changes and remove the branch on GitHub.
   Then, also remove the branch locally:
   > `git checkout master; git branch -D update-windows-tz-names`

11. Follow the procedure for publishing a new release
   (see [RELEASE.md](RELEASE.md)) if new timezone names were added.
