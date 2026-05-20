# College Placement System — API Route Map

## Architecture: 3 Portals

Each portal is a separate Maven module with its own router, mounted in HttpVerticle.

---

## College Portal (`/college`) — TPO / College Admin

### College Management
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/college/create` | CreateCollegeController | Create college (SUPER_ADMIN) |
| GET | `/college/list` | ListCollegesController | List all colleges |
| GET | `/college/me` | GetCollegeController | Get own college |
| PUT | `/college/me` | UpdateCollegeController | Update own college |

### Placement Policy
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/college/policy` | CreatePolicyController | Create placement policy |
| GET | `/college/policy` | GetPolicyController | Get policy (latest or by year) |

### Documents
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/college/documents` | UploadDocumentController | Upload document |
| GET | `/college/documents` | ListDocumentsController | List documents |

### Analytics
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/college/analytics` | CollegeAnalyticsController | Placement stats |

### Student Management
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/college/students` | ListStudentsController | All students |
| GET | `/college/students/placed` | ListPlacedStudentsController | Placed students |
| GET | `/college/students/unplaced` | ListUnplacedStudentsController | Unplaced students |
| GET | `/college/students/unverified` | ListUnverifiedStudentsController | Pending verification |
| POST | `/college/students/:studentId/verify` | VerifyStudentController | Verify a student |

### Company Management
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/college/companies/link` | LinkCompanyCollegeController | Link company to college |
| GET | `/college/companies` | ListCompanyCollegesController | List linked companies |

### Drive Management
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/college/drives` | CreateDriveController | Create drive |
| GET | `/college/drives` | ListDrivesController | List drives (?year=) |
| GET | `/college/drives/upcoming` | ListUpcomingDrivesController | Upcoming drives |
| PUT | `/college/drives/:driveId` | UpdateDriveController | Update drive |
| GET | `/college/drives/:driveId/applications` | ListDriveApplicationsController | View applications (?status=) |
| POST | `/college/drives/:driveId/rounds` | CreateRoundController | Create round |
| GET | `/college/drives/:driveId/rounds` | ListRoundsController | List rounds |
| POST | `/college/rounds/:roundId/results` | SubmitRoundResultsController | Submit round result |
| POST | `/college/drives/:driveId/offers` | CreateOfferController | Create offer |
| GET | `/college/drives/:driveId/offers` | ListDriveOffersController | List offers |

---

## Student Portal (`/student`) — Students

### Onboarding & Profile
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/student/onboard` | StudentOnboardController | Create academic profile |
| GET | `/student/me` | GetMyProfileController | Get own profile |
| PUT | `/student/me` | UpdateProfileController | Update own profile |

### Applications & Offers
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/student/me/applications` | MyApplicationsController | My applications |
| GET | `/student/me/offers` | MyOffersController | My offers |
| POST | `/student/me/offers/:offerId/respond` | RespondToOfferController | Accept/decline offer |

### Drives (Browse & Apply)
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/student/drives` | ListAvailableDrivesController | Browse available drives |
| GET | `/student/drives/:driveId` | GetDriveDetailController | View drive details |
| POST | `/student/drives/:driveId/apply` | ApplyToDriveController | Apply to drive |

### PYQ (Previous Year Questions)
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/student/pyq/company/:companyId` | GetPYQController | View PYQs (?roundType=) |
| POST | `/student/pyq/contribute` | ContributePYQController | Contribute a PYQ |

---

## Company Portal (`/company`) — Company HR

### Company Profile
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| POST | `/company/create` | CreateCompanyController | Create company |
| GET | `/company/list` | ListCompaniesController | List all companies |
| GET | `/company/:id` | GetCompanyController | Get company by ID |

### Company Dashboard
| Method | Route | Controller | Description |
|--------|-------|------------|-------------|
| GET | `/company/:companyId/colleges` | ListLinkedCollegesController | Colleges linked to company |
| GET | `/company/:companyId/drives` | ListCompanyDrivesController | Drives across all colleges |

---

## Core Modules

### User (`/user`)
Signup, profile management. College code required for student/TPO/admin signup.

### Auth (`/auth`)
JWT authentication via Auth0.

---

## Key Design Decisions

- **College is the tenant** — all student/drive/offer data is scoped to a college
- **No `/drive` route prefix** — drive management is split between college (admin) and student (apply) portals
- **TPO operations always use `request.getUser().college.getId()`** — no `:collegeId` path param needed
- **Company HR sees cross-college data** — their drives span multiple linked colleges
- **Verification flow**: Signup → Onboard → TPO Verify → Apply to drives
