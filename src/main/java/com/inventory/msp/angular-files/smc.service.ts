import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SmcService {
  private apiUrl = 'http://localhost:8080/api'; // Adjust to your backend URL

  constructor(private http: HttpClient) {}

  // ==================== WEIGHBRIDGE ENDPOINTS ====================

  // Get all weighbridge data (for table filtering)
  getAllWeighbridgeData(): Observable<any> {
    return this.http.get(`${this.apiUrl}/weighbridge/data/all`);
  }

  // Get weighbridge data within timeframe (with START and END date filtering)
  getTimeframeData(startDate: string, endDate: string, wbId: string): Observable<any> {
    const params = new HttpParams()
      .set('start', startDate)
      .set('end', endDate)
      .set('wbId', wbId);

    return this.http.get(`${this.apiUrl}/weighbridge/timeframe/data`, { params });
  }

  // Get net weight trend (NOW WITH DATE FILTERING)
  getNetTrend(wbId: string, startDate?: string, endDate?: string): Observable<any> {
    if (startDate && endDate) {
      // Use timeframe endpoint if custom dates provided
      return this.getTimeframeData(startDate, endDate, wbId);
    }
    // Fallback to trend endpoint for historical data (no date filter)
    return this.http.get(`${this.apiUrl}/weighbridge/report/trend/net/${wbId}`);
  }

  // Get gross weight trend
  getGrossTrend(wbId: string, startDate?: string, endDate?: string): Observable<any> {
    if (startDate && endDate) {
      return this.getTimeframeData(startDate, endDate, wbId);
    }
    return this.http.get(`${this.apiUrl}/weighbridge/report/trend/gross/${wbId}`);
  }

  // Get vehicle trend (grouped by vehicle number)
  getVehicleTrend(wbId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/weighbridge/report/trend/vehicle/${wbId}`);
  }

  // Get last 24 hours data (NOW WITH DATE FILTERING)
  getLast24Trend(wbId: string, startDate?: string, endDate?: string): Observable<any> {
    if (startDate && endDate) {
      // Use custom timeframe if provided
      return this.getTimeframeData(startDate, endDate, wbId);
    }
    // Fallback to last 24 hours endpoint
    return this.http.get(`${this.apiUrl}/weighbridge/report/trend/last24/${wbId}`);
  }

  // Get daily summary (for date range reports)
  getDailySummary(date: string, wbId: string): Observable<any> {
    const params = new HttpParams()
      .set('date', date)
      .set('wbId', wbId);

    return this.http.get(`${this.apiUrl}/weighbridge/report/summary/day`, { params });
  }

  // Get summary for custom date range
  getSummary(startDate: string, endDate: string, wbId: string): Observable<any> {
    const params = new HttpParams()
      .set('start', startDate)
      .set('end', endDate)
      .set('wbId', wbId);

    return this.http.get(`${this.apiUrl}/weighbridge/report/summary`, { params });
  }

  // Sync weighbridge data from external API
  syncWeighbridgeData(): Observable<any> {
    return this.http.get(`${this.apiUrl}/weighbridge/sync`);
  }

  // ==================== CHARTERED BIKE ENDPOINTS ====================

  // Get chartered bike stations
  getCharteredBikeStations(): Observable<any> {
    return this.http.get(`${this.apiUrl}/chartered-bike/stations`);
  }

  // Get chartered bike stations with minimum bikes filter
  getCharteredBikeStationsFiltered(minBikes: number): Observable<any> {
    const params = new HttpParams().set('minBikes', minBikes.toString());
    return this.http.get(`${this.apiUrl}/chartered-bike/stations`, { params });
  }

  // ==================== OTHER ENDPOINTS ====================

  // Add other service methods as needed for your dashboard
}

