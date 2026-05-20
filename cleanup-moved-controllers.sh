#!/bin/bash
# Run this script to remove controllers that were moved to their proper portal modules.
# These files are now dead code — their functionality lives in the correct module.

echo "Removing old student-module controllers (moved to college module)..."
rm -f student/src/main/java/student/ListStudentsController.java
rm -f student/src/main/java/student/ListPlacedStudentsController.java
rm -f student/src/main/java/student/ListUnplacedStudentsController.java
rm -f student/src/main/java/student/ListUnverifiedStudentsController.java
rm -f student/src/main/java/student/VerifyStudentController.java
rm -f student/src/main/java/student/GetStudentController.java

echo "Removing old company-module controllers (moved to college module)..."
rm -f company/src/main/java/company/LinkCompanyCollegeController.java
rm -f company/src/main/java/company/ListCompanyCollegesController.java

echo "Removing old drive-module controllers (moved to college/student modules)..."
rm -f drive/src/main/java/drive/CreateDriveController.java
rm -f drive/src/main/java/drive/UpdateDriveController.java
rm -f drive/src/main/java/drive/ListDrivesController.java
rm -f drive/src/main/java/drive/ListUpcomingDrivesController.java
rm -f drive/src/main/java/drive/ListDriveApplicationsController.java
rm -f drive/src/main/java/drive/CreateRoundController.java
rm -f drive/src/main/java/drive/ListRoundsController.java
rm -f drive/src/main/java/drive/SubmitRoundResultsController.java
rm -f drive/src/main/java/drive/CreateOfferController.java
rm -f drive/src/main/java/drive/ListDriveOffersController.java
rm -f drive/src/main/java/drive/ApplyToDriveController.java
rm -f drive/src/main/java/drive/GetDriveController.java

echo ""
echo "Optionally remove the entire drive module (now empty)..."
echo "  rm -rf drive/"
echo "  Then remove <module>drive</module> from the parent pom.xml"
echo ""

echo "Done! Old controllers removed."
echo ""
echo "New structure (3 portals):"
echo "  /college — College portal (TPO/Admin): students, companies, drives, rounds, offers, policy, analytics"
echo "  /student — Student portal: onboard, profile, applications, offers, drives (browse+apply), PYQ"
echo "  /company — Company portal: profile CRUD, view linked colleges, view their drives"
