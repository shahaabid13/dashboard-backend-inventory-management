import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpClientModule, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { catchError, finalize, of } from 'rxjs';

interface DashboardWidget<T> {
  data: T;
  loading: boolean;
  error: string | null;
}

interface DashboardSummary {
  totalInQueue: number;
  flaggedCount: number;
  avgAgeDays: number;
  oldestAgeDays: number;
  tasksPending: number;
  tasksOverdue: number;
  tasksCompletedThisWeek: number;
}

interface TicketTrend {
  date: string;
  created: number;
  closed: number;
}

@Component({
  selector: 'app-cims-dashboard',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './cims-dashboard.component.html'
})
export class CimsDashboardComponent implements OnInit {
  readonly summary: DashboardWidget<DashboardSummary | null> = this.widget(null);
  readonly ticketsByAssignee: DashboardWidget<Array<{ userName: string; count: number }>> = this.widget([]);
  readonly ticketsByStatus: DashboardWidget<Array<{ status: string; count: number }>> = this.widget([]);
  readonly ticketsByIncidentType: DashboardWidget<Array<{ type: string; count: number }>> = this.widget([]);
  readonly ageingBuckets: DashboardWidget<Array<{ bucket: string; count: number }>> = this.widget([]);
  readonly ticketTrend: DashboardWidget<TicketTrend[]> = this.widget([]);
  readonly tasksByStatus: DashboardWidget<Array<{ status: string; count: number }>> = this.widget([]);
  readonly tasksByAssignee: DashboardWidget<Array<{ userName: string; pending: number; overdue: number }>> = this.widget([]);

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadSummary();
    this.loadTicketsByAssignee();
    this.loadTicketsByStatus();
    this.loadTicketsByIncidentType();
    this.loadAgeingBuckets();
    this.loadTicketTrend();
    this.loadTasksByStatus();
    this.loadTasksByAssignee();
  }

  loadSummary(): void {
    this.load(this.summary, '/api/cims/dashboard/summary', null);
  }

  loadTicketsByAssignee(): void {
    this.load(this.ticketsByAssignee, '/api/cims/dashboard/tickets-by-assignee', []);
  }

  loadTicketsByStatus(): void {
    this.load(this.ticketsByStatus, '/api/cims/dashboard/tickets-by-status', []);
  }

  loadTicketsByIncidentType(): void {
    this.load(this.ticketsByIncidentType, '/api/cims/dashboard/tickets-by-incident-type', []);
  }

  loadAgeingBuckets(): void {
    this.load(this.ageingBuckets, '/api/cims/dashboard/tickets-ageing-buckets', []);
  }

  loadTicketTrend(): void {
    this.load(this.ticketTrend, '/api/cims/dashboard/tickets-trend?days=30', []);
  }

  loadTasksByStatus(): void {
    this.load(this.tasksByStatus, '/api/cims/dashboard/tasks-by-status', []);
  }

  loadTasksByAssignee(): void {
    this.load(this.tasksByAssignee, '/api/cims/dashboard/tasks-by-assignee', []);
  }

  private widget<T>(data: T): DashboardWidget<T> {
    return { data, loading: false, error: null };
  }

  private load<T>(state: DashboardWidget<T>, url: string, emptyValue: T): void {
    state.loading = true;
    state.error = null;
    const token = localStorage.getItem('token');
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();

    this.http.get<T>(url, { headers }).pipe(
      catchError((error: HttpErrorResponse) => {
        state.error = error.status === 0
          ? 'Unable to reach the dashboard service.'
          : `Could not load this widget (${error.status}).`;
        return of(emptyValue);
      }),
      finalize(() => state.loading = false)
    ).subscribe(data => state.data = data);
  }
}
