import { EmploymentType, PersonRef } from '../requisitions/requisition.models';

export interface PublicOrganization {
  name: string;
  slug: string;
}

/** An open posting on the public careers site (`/v1/public/organizations/{slug}/jobs`). */
export interface PublicJob {
  id: string;
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  description: string | null;
  publishedAt: string;
  organization: PublicOrganization;
}

export interface PublicJobList {
  organization: PublicOrganization;
  jobs: PublicJob[];
}

export interface ApplicationRequest {
  fullName: string;
  email: string;
  phone: string | null;
  coverLetter: string | null;
  privacyConsent: boolean;
}

/** Same reply for a first and a repeated application. */
export interface ApplicationReceipt {
  status: 'received';
  jobTitle: string;
  organizationName: string;
}

export type PostingStatus = 'open' | 'closed';

/** A posting as recruiters see it (`/v1/organizations/{id}/jobs`). */
export interface JobPosting {
  id: string;
  requisitionId: string;
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  status: PostingStatus;
  publishedBy: PersonRef;
  publishedAt: string;
  closedBy: PersonRef | null;
  closedAt: string | null;
  applicationCount: number;
  /** In-app path of the public page, e.g. `/careers/northwind-talent/jobs/…`. */
  publicPath: string;
  version: number;
}
