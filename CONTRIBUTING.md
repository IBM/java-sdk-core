# Issues

If you encounter an issue with the project, you are welcome to submit a [bug report](https://github.com/IBM/java-sdk-core/issues).
Before that, please search for similar issues. It's possible that someone has already reported the problem.

# Pull Requests

If you want to contribute to the repository, here's a quick guide:
  1. Fork the repository
  2. Develop and test your code changes:
      * To build/test: `mvn clean package`
      * Please add one or more tests to validate your changes.
  3. Make sure everything builds/tests cleanly
  4. Commit your changes  
  5. Push to your fork and submit a pull request to the **master** branch

# Creating a release

To create a release from the most recent commit in the master branch, follow these steps:
  1. In your local copy of the repo (a clone, not a fork), checkout the master branch:
     ```
          git checkout master
     ```
  2. Add the tag (this example creates the 1.2.0 tag):
     ```
          git tag 1.2.0
     ```
     Note: specify the appropriate three-level version # (1.2.0, 1.3.1, 2.0.0, etc.)
  3. Push the tag to remote:
     ```
          git push --tags
     ```
     This will trigger a tagged build in Travis, which will perform the deployment steps to deploy the build outputs to bintray (and maven central) and the github project's `Releases` page.


# Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
   have the right to submit it under the open source license
   indicated in the file; or

(b) The contribution is based upon previous work that, to the best
   of my knowledge, is covered under an appropriate open source
   license and I have the right under that license to submit that
   work with modifications, whether created in whole or in part
   by me, under the same open source license (unless I am
   permitted to submit under a different license), as indicated
   in the file; or

(c) The contribution was provided directly to me by some other
   person who certified (a), (b) or (c) and I have not modified
   it.

(d) I understand and agree that this project and the contribution
   are public and that a record of the contribution (including all
   personal information I submit with it, including my sign-off) is
   maintained indefinitely and may be redistributed consistent with
   this project or the open source license(s) involved.

## Additional Resources
+ [General GitHub documentation](https://help.github.com/)
+ [GitHub pull request documentation](https://help.github.com/send-pull-requests/)

[Maven]: https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html
