# How to Submit Patches to the Catima Project

This document is intended to act as a guide to help you contribute to the
Catima project. It is not perfect, and there will always be exceptions
to the rules described here, but by following the instructions below you
should have a much easier time getting your work merged with the upstream
project.

When contributing, you certify that you agree to and have the rights to submit
your contribution under the project's license and understand that git will
store your name and email address in project history indefinitely.

## Translation Changes

Translation changes are managed through [Weblate](https://hosted.weblate.org/projects/catima/).
Please do not supply translation updates directly through GitHub.

Weblate requires an account to translate changes, so please log in before
you start translating.

While using Weblate, please do not ignore any of its warnings. They exist
for good reason.

## Code Changes

### Test Your Code

There are four possible tests you can run to verify your code.  The first
is unit tests, which check the basic functionality of the application, and
can be run by gradle using:

    # ./gradlew testReleaseUnitTest

The second and third check for common problems using static analysis.
These are the Android lint checker, run using:

    # ./gradlew lintRelease

The final check is by testing the application on a live device and verifying
the basic functionality works as expected.

### Make Sure Your Code is Tested

The Catima code uses a fair number of unit tests to verify that
the basic functionality is working. Submissions which add functionality
or significantly change the existing code should include additional tests
to verify the proper operation of the proposed changes.

### Explain Your Work

At the top of every patch you should include a description of the problem you
are trying to solve, how you solved it, and why you chose the solution you
implemented.  If you are submitting a bug fix, it is also incredibly helpful
if you can describe/include a reproducer for the problem in the description as
well as instructions on how to test for the bug and verify that it has been
fixed.

### Submit Patch(es) for Review

Finally, you will need to submit your patches so that they can be reviewed
and potentially merged into the main Catima repository. The preferred
way to do this is to submit a Pull Request to the Catima project.
Changes need to apply cleanly onto the main branch and pass all
unit tests and produce no errors during static analysis.
