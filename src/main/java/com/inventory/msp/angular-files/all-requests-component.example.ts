/**
 * SIMPLIFIED COMPONENT USING DateFormatterUtil
 *
 * This is a refactored version of your all-requests component
 * that uses the shared DateFormatterUtil instead of duplicating the date parsing logic.
 *
 * You can copy the relevant sections from this file into your actual component.
 */

import { Component, OnInit, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { HistoryService } from '../../../services/history.service';
import { DateFormatterUtil } from '../date-formatter.util'; // ← Import the utility
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';

export interface MaintenanceRequest {
  formattedUpdatedAt: any;
  formattedCreatedAt: any;
  normalizedStatus: string;
  id: number;
  deviceId: number;
  oldSerial: string;
  newSerial: string | null;
  newLocationId: number | null;
  newApproachRoadId: number | null;
  requestType: string;
  status: string;
  createdBy: string;
  approvedBy: string | null;
  referenceId: string;
  remarks: string | null;
  createdAt: any;
  updatedAt: any;
}

// ... ConfirmDialog component (unchanged) ...

@Component({
  selector: 'app-all-requests',
  standalone: true,
  imports: [
    CommonModule, HttpClientModule, MatTableModule, MatCardModule, MatButtonModule,
    MatSnackBarModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule,
    MatDialogModule, MatSlideToggleModule, MatChipsModule, MatSelectModule,
    MatFormFieldModule, FormsModule
  ],
  templateUrl: './all-requests.html',
  styleUrls: ['./all-requests.scss']
})
export class AllRequestsComponent implements OnInit {
  displayedColumns: string[] = [
    'id', 'deviceId', 'oldSerial', 'newSerial', 'requestType', 'status',
    'createdBy', 'approvedBy', 'referenceId', 'remarks', 'createdAt', 'updatedAt', 'actions'
  ];

  requests: MaintenanceRequest[] = [];
  filteredRequests: MaintenanceRequest[] = [];
  role: string = '';
  username: string = '';
  loading = false;
  actionInProgress: number | null = null;
  selectedStatus: string = 'all';

  private apiUrl = '/api/maintenance/requests';

  constructor(
    private snackBar: MatSnackBar,
    private http: HttpClient,
    private dialog: MatDialog,
    private historyService: HistoryService
  ) {}

  ngOnInit(): void {
    this.role = localStorage.getItem('role') || 'agency';
    this.username = localStorage.getItem('username') || '';

    const normalizedRole = this.role.toLowerCase();
    if (normalizedRole !== 'admin' && this.displayedColumns.includes('actions')) {
      this.displayedColumns = this.displayedColumns.filter(col => col !== 'actions');
    }

    this.loadRequests();
  }

  /**
   * NOW: Just delegate to DateFormatterUtil.formatDate()
   * No need to duplicate parseDate logic anymore!
   */
  formatDate(input: any): string {
    return DateFormatterUtil.formatDate(input);
  }

  loadRequests(): void {
    this.loading = true;

    const token = localStorage.getItem('token');
    if (!token) {
      this.snackBar.open('❌ You must log in to view maintenance requests', 'Close',
        { duration: 3000, panelClass: ['error-snackbar'] });
      this.loading = false;
      return;
    }

    const headers = { headers: { Authorization: `Bearer ${token}` } } as any;

    this.http.get<any[]>(this.apiUrl, headers).subscribe({
      next: (data: any) => {
        const records: any[] = Array.isArray(data) ? data : [];

        // Log raw response (useful for debugging)
        if (records.length) {
          console.log('[AllRequests] First raw record:', JSON.stringify(records[0]));
          console.log('[AllRequests] createdAt raw:', records[0].createdAt,
            '| type:', typeof records[0].createdAt);
        }

        // ← USE DateFormatterUtil.enrichWithFormattedDates() to add formatted date fields
        this.requests = records.map(request => ({
          ...request,
          normalizedStatus: request.status?.toLowerCase() || 'pending',
          // ← Simplified: use utility to format dates
          formattedCreatedAt: DateFormatterUtil.formatDate(request.createdAt),
          formattedUpdatedAt: DateFormatterUtil.formatDate(request.updatedAt),
          displayApprovedBy: request.approvedBy || 'Not approved',
          displayNewSerial: request.newSerial || 'N/A',
          displayRemarks: request.remarks || 'No remarks'
        }));

        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching maintenance requests:', err);
        this.snackBar.open('❌ Failed to load maintenance requests', 'Close',
          { duration: 3000, panelClass: ['error-snackbar'] });
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    if (this.selectedStatus === 'all') {
      this.filteredRequests = [...this.requests];
    } else {
      this.filteredRequests = this.requests.filter(r =>
        r.normalizedStatus === this.selectedStatus.toLowerCase()
      );
    }
  }

  clearFilter(): void {
    this.selectedStatus = 'all';
    this.applyFilter();
  }

  getRequestsByStatus(status: string): MaintenanceRequest[] {
    return this.requests.filter(r => r.normalizedStatus === status.toLowerCase());
  }

  getStatusIcon(status: string): string {
    switch (status?.toLowerCase()) {
      case 'approved': return 'check_circle';
      case 'rejected': return 'cancel';
      case 'pending': return 'pending';
      default: return 'help';
    }
  }

  confirmAction(request: MaintenanceRequest, approve: boolean): void {
    this.historyService.getDeviceHistory(request.deviceId).subscribe({
      next: (history: any[]) => this.openConfirmDialog(request, approve,
        Array.isArray(history) ? history.slice(0, 3) : []),
      error: () => this.openConfirmDialog(request, approve, [])
    });
  }

  private openConfirmDialog(request: MaintenanceRequest, approve: boolean, deviceHistory: any[]): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '600px',
      data: {
        title: approve ? 'Approve Request' : 'Reject Request',
        message: `Are you sure you want to ${approve ? 'approve' : 'reject'} request #${request.id}?`,
        action: approve ? 'Approve' : 'Reject',
        color: approve ? 'primary' : 'warn',
        request,
        deviceHistory
      }
    });
    dialogRef.afterClosed().subscribe(result => { if (result) this.submitAction(request, approve); });
  }

  submitAction(request: MaintenanceRequest, approved: boolean): void {
    this.actionInProgress = request.id;

    const token = localStorage.getItem('token');
    if (!token) {
      this.snackBar.open('❌ You must log in to perform this action', 'Close',
        { duration: 3000, panelClass: ['error-snackbar'] });
      this.actionInProgress = null;
      return;
    }

    const headers = { headers: { Authorization: `Bearer ${token}` } } as any;
    const payload = {
      approvedBy: this.username,
      approved,
      remarks: approved ? 'Request approved by administrator' : 'Request rejected by administrator'
    };

    this.http.post(`${this.apiUrl}/${request.id}/approve`, payload, headers).subscribe({
      next: () => {
        this.snackBar.open(`✅ Request #${request.id} ${approved ? 'approved' : 'rejected'} successfully`,
          'Close', { duration: 3000, panelClass: ['success-snackbar'] });
        this.actionInProgress = null;
        this.loadRequests();
      },
      error: (err) => {
        const msg = err.error?.message || (err.status === 404 ? 'Request not found' :
          err.status === 403 ? 'Permission denied' : 'Failed to process request');
        this.snackBar.open(`❌ ${msg}`, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
        this.actionInProgress = null;
      }
    });
  }

  isAdmin(): boolean { return this.role.toLowerCase() === 'admin'; }
  canPerformAction(request: MaintenanceRequest): boolean { return request.normalizedStatus === 'pending'; }
  getRequestTypeColor(requestType: string): string {
    switch (requestType?.toLowerCase()) {
      case 'replace': return '#f54656';
      case 'repair': return '#ff9800';
      case 'fault': return '#9c27b0';
      case 'serial_update': return '#2196f3';
      case 'move': return '#4caf50';
      default: return '#757575';
    }
  }
}

