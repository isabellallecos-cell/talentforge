import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  PersonRef,
  Requisition,
  RequisitionEvent,
  RequisitionRequest,
  RequisitionStatus,
  StatusAction,
} from './requisition.models';

/** Thin client for `/v1/organizations/{id}/requisitions`. */
@Injectable({ providedIn: 'root' })
export class RequisitionsApi {
  private readonly http = inject(HttpClient);

  list(organizationId: string, status: RequisitionStatus | null): Observable<Requisition[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Requisition[]>(this.base(organizationId), { params });
  }

  get(organizationId: string, id: string): Observable<Requisition> {
    return this.http.get<Requisition>(`${this.base(organizationId)}/${encodeURIComponent(id)}`);
  }

  history(organizationId: string, id: string): Observable<RequisitionEvent[]> {
    return this.http.get<RequisitionEvent[]>(`${this.base(organizationId)}/${encodeURIComponent(id)}/events`);
  }

  hiringManagers(organizationId: string): Observable<PersonRef[]> {
    return this.http.get<PersonRef[]>(`${this.base(organizationId)}/hiring-managers`);
  }

  create(organizationId: string, body: RequisitionRequest): Observable<Requisition> {
    return this.http.post<Requisition>(this.base(organizationId), body);
  }

  update(organizationId: string, id: string, body: RequisitionRequest): Observable<Requisition> {
    return this.http.put<Requisition>(`${this.base(organizationId)}/${encodeURIComponent(id)}`, body);
  }

  transition(
    organizationId: string,
    id: string,
    action: StatusAction,
    version: number,
    note: string | null,
  ): Observable<Requisition> {
    return this.http.post<Requisition>(`${this.base(organizationId)}/${encodeURIComponent(id)}/${action}`, {
      version,
      note,
    });
  }

  private base(organizationId: string): string {
    return `/v1/organizations/${encodeURIComponent(organizationId)}/requisitions`;
  }
}
