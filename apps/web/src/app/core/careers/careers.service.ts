import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationReceipt, ApplicationRequest, JobPosting, PublicJob, PublicJobList } from './careers.models';

/** Public careers site: no session needed (`/v1/public/organizations/{slug}/jobs`). */
@Injectable({ providedIn: 'root' })
export class CareersApi {
  private readonly http = inject(HttpClient);

  list(slug: string): Observable<PublicJobList> {
    return this.http.get<PublicJobList>(this.base(slug));
  }

  get(slug: string, jobId: string): Observable<PublicJob> {
    return this.http.get<PublicJob>(`${this.base(slug)}/${encodeURIComponent(jobId)}`);
  }

  apply(slug: string, jobId: string, body: ApplicationRequest): Observable<ApplicationReceipt> {
    return this.http.post<ApplicationReceipt>(`${this.base(slug)}/${encodeURIComponent(jobId)}/applications`, body);
  }

  private base(slug: string): string {
    return `/v1/public/organizations/${encodeURIComponent(slug)}/jobs`;
  }
}

/** Recruiter side: publish approved requisitions and close postings (`/v1/organizations/{id}/jobs`). */
@Injectable({ providedIn: 'root' })
export class JobPostingsApi {
  private readonly http = inject(HttpClient);

  forRequisition(organizationId: string, requisitionId: string): Observable<JobPosting[]> {
    return this.http.get<JobPosting[]>(this.base(organizationId), {
      params: new HttpParams().set('requisitionId', requisitionId),
    });
  }

  publish(organizationId: string, requisitionId: string): Observable<JobPosting> {
    return this.http.post<JobPosting>(this.base(organizationId), { requisitionId });
  }

  close(organizationId: string, postingId: string, version: number): Observable<JobPosting> {
    return this.http.post<JobPosting>(`${this.base(organizationId)}/${encodeURIComponent(postingId)}/close`, {
      version,
    });
  }

  private base(organizationId: string): string {
    return `/v1/organizations/${encodeURIComponent(organizationId)}/jobs`;
  }
}
