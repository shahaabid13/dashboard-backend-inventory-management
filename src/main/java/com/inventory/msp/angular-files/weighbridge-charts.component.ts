import { Component, OnInit } from '@angular/core';
import { SmcService } from '../services/smc.service';
import { ChartOptions, ChartData } from 'chart.js';

@Component({
  selector: 'app-weighbridge-charts',
  templateUrl: './weighbridge-charts.component.html',
  styleUrls: ['./weighbridge-charts.component.css']
})
export class WeighbridgeChartsComponent implements OnInit {

  // ==================== DATE FILTERING ====================
  startDate: string = new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0]; // Last 7 days
  endDate: string = new Date().toISOString().split('T')[0]; // Today
  selectedWbId: string = ''; // Will be set from dropdown
  wbIdOptions: any[] = []; // Populated from API or static data

  // ==================== LOADING & ERROR STATES ====================
  isLoadingCharts = false;
  chartError: string = '';

  // ==================== NET WEIGHT CHART ====================
  netWeightChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Net Weight Trend (kg)',
        data: [],
        borderColor: '#3366cc',
        backgroundColor: 'rgba(51, 102, 204, 0.1)',
        borderWidth: 2,
        tension: 0.4,
        fill: true,
        pointRadius: 4,
        pointBackgroundColor: '#3366cc',
        pointBorderColor: '#fff',
        pointBorderWidth: 1
      }
    ]
  };

  netWeightChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top'
      },
      title: {
        display: true,
        text: 'Net Weight Trend'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Net Weight (kg)'
        }
      },
      x: {
        title: {
          display: true,
          text: 'Date/Time'
        }
      }
    }
  };

  // ==================== GROSS WEIGHT CHART ====================
  grossWeightChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Gross Weight Trend (kg)',
        data: [],
        borderColor: '#ff7300',
        backgroundColor: 'rgba(255, 115, 0, 0.1)',
        borderWidth: 2,
        tension: 0.4,
        fill: true,
        pointRadius: 4,
        pointBackgroundColor: '#ff7300',
        pointBorderColor: '#fff',
        pointBorderWidth: 1
      }
    ]
  };

  grossWeightChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'top'
      },
      title: {
        display: true,
        text: 'Gross Weight Trend'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Gross Weight (kg)'
        }
      }
    }
  };

  // ==================== LAST 24 HOURS CHART ====================
  last24HoursChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Last 24 Hours',
        data: [],
        backgroundColor: 'rgba(76, 175, 80, 0.8)',
        borderColor: '#4caf50',
        borderWidth: 1
      }
    ]
  };

  last24HoursChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'x' as const,
    plugins: {
      legend: {
        display: true
      },
      title: {
        display: true,
        text: 'Last 24 Hours Activity'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Number of Trips'
        }
      }
    }
  };

  // ==================== VEHICLE TREND CHART ====================
  vehicleTrendChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Net Weight by Vehicle (kg)',
        data: [],
        backgroundColor: [
          '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
          '#FF9F40', '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0'
        ],
        borderWidth: 1
      }
    ]
  };

  vehicleTrendChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y' as const,
    plugins: {
      legend: {
        display: true
      },
      title: {
        display: true,
        text: 'Top Vehicles by Net Weight'
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Total Net Weight (kg)'
        }
      }
    }
  };

  constructor(private smcService: SmcService) {}

  ngOnInit(): void {
    // Initialize weighbridge IDs (you can fetch from API or set static values)
    this.wbIdOptions = [
      { id: 'WB001', name: 'Weighbridge 1' },
      { id: 'WB002', name: 'Weighbridge 2' },
      { id: 'WB003', name: 'Weighbridge 3' }
    ];

    // Set default weighbridge
    if (this.wbIdOptions.length > 0) {
      this.selectedWbId = this.wbIdOptions[0].id;
      this.loadCharts();
    }
  }

  // ==================== DATE FILTER HANDLER ====================
  /**
   * Called when user changes start date, end date, or weighbridge selection
   * Reloads all charts with the new filter criteria
   */
  onDateOrWbChange(): void {
    if (this.selectedWbId) {
      this.loadCharts();
    }
  }

  // ==================== LOAD ALL CHARTS ====================
  loadCharts(): void {
    this.isLoadingCharts = true;
    this.chartError = '';

    // Load all charts in parallel
    Promise.all([
      this.loadNetWeightChart(),
      this.loadGrossWeightChart(),
      this.loadLast24HoursChart(),
      this.loadVehicleTrendChart()
    ]).then(() => {
      this.isLoadingCharts = false;
    }).catch(error => {
      console.error('Error loading charts:', error);
      this.chartError = 'Failed to load all charts. Please try again.';
      this.isLoadingCharts = false;
    });
  }

  // ==================== NET WEIGHT CHART ====================
  loadNetWeightChart(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.smcService.getNetTrend(this.selectedWbId, this.startDate, this.endDate)
        .subscribe({
          next: (data) => {
            this.processNetWeightData(data);
            resolve();
          },
          error: (error) => {
            console.error('Error loading net weight chart:', error);
            this.chartError = 'Failed to load net weight chart';
            reject(error);
          }
        });
    });
  }

  private processNetWeightData(data: any[]): void {
    if (!data || data.length === 0) {
      this.netWeightChartData.labels = [];
      this.netWeightChartData.datasets[0].data = [];
      return;
    }

    const labels: string[] = [];
    const weights: number[] = [];

    data.forEach((entry: any) => {
      // Handle both object format and raw format
      const date = entry.date || entry.edate || '';
      const time = entry.time || '';
      const weight = entry.nweight || entry.data?.[1] || 0;

      labels.push(`${date} ${time}`.trim());
      weights.push(Number(weight));
    });

    this.netWeightChartData.labels = labels;
    this.netWeightChartData.datasets[0].data = weights;
  }

  // ==================== GROSS WEIGHT CHART ====================
  loadGrossWeightChart(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.smcService.getGrossTrend(this.selectedWbId, this.startDate, this.endDate)
        .subscribe({
          next: (data) => {
            this.processGrossWeightData(data);
            resolve();
          },
          error: (error) => {
            console.error('Error loading gross weight chart:', error);
            this.chartError = 'Failed to load gross weight chart';
            reject(error);
          }
        });
    });
  }

  private processGrossWeightData(data: any[]): void {
    if (!data || data.length === 0) {
      this.grossWeightChartData.labels = [];
      this.grossWeightChartData.datasets[0].data = [];
      return;
    }

    const labels: string[] = [];
    const weights: number[] = [];

    data.forEach((entry: any) => {
      const date = entry.date || entry.edate || '';
      const time = entry.time || '';
      const weight = entry.gweight || entry.data?.[1] || 0;

      labels.push(`${date} ${time}`.trim());
      weights.push(Number(weight));
    });

    this.grossWeightChartData.labels = labels;
    this.grossWeightChartData.datasets[0].data = weights;
  }

  // ==================== LAST 24 HOURS CHART ====================
  loadLast24HoursChart(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.smcService.getLast24Trend(this.selectedWbId, this.startDate, this.endDate)
        .subscribe({
          next: (data) => {
            this.processLast24HoursData(data);
            resolve();
          },
          error: (error) => {
            console.error('Error loading last 24 hours chart:', error);
            // Don't reject on error, just show empty chart
            resolve();
          }
        });
    });
  }

  private processLast24HoursData(data: any[]): void {
    if (!data || data.length === 0) {
      this.last24HoursChartData.labels = [];
      this.last24HoursChartData.datasets[0].data = [];
      return;
    }

    const labels: string[] = [];
    const counts: number[] = [];

    // Group data by hour
    const hourlyData: { [key: string]: number } = {};

    data.forEach((entry: any) => {
      const date = entry.date || entry.edate || '';
      const time = entry.time || '';
      const hour = time.split(':')[0]; // Get hour from time
      const key = `${date} ${hour}:00`;

      hourlyData[key] = (hourlyData[key] || 0) + 1;
    });

    Object.keys(hourlyData).sort().forEach(key => {
      labels.push(key);
      counts.push(hourlyData[key]);
    });

    this.last24HoursChartData.labels = labels;
    this.last24HoursChartData.datasets[0].data = counts;
  }

  // ==================== VEHICLE TREND CHART ====================
  loadVehicleTrendChart(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.smcService.getVehicleTrend(this.selectedWbId)
        .subscribe({
          next: (data) => {
            this.processVehicleTrendData(data);
            resolve();
          },
          error: (error) => {
            console.error('Error loading vehicle trend chart:', error);
            // Don't reject on error
            resolve();
          }
        });
    });
  }

  private processVehicleTrendData(data: any[]): void {
    if (!data || data.length === 0) {
      this.vehicleTrendChartData.labels = [];
      this.vehicleTrendChartData.datasets[0].data = [];
      return;
    }

    const labels: string[] = [];
    const weights: number[] = [];

    data.forEach((entry: any) => {
      const vehicleNo = entry.data?.[0] || entry.vno || 'Unknown';
      const weight = entry.data?.[1] || entry.weight || 0;

      labels.push(String(vehicleNo));
      weights.push(Number(weight));
    });

    this.vehicleTrendChartData.labels = labels;
    this.vehicleTrendChartData.datasets[0].data = weights;
  }

  // ==================== UTILITY METHODS ====================
  /**
   * Reset filters to default (last 7 days)
   */
  resetFilters(): void {
    const today = new Date();
    const sevenDaysAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);

    this.endDate = today.toISOString().split('T')[0];
    this.startDate = sevenDaysAgo.toISOString().split('T')[0];

    this.loadCharts();
  }

  /**
   * Export chart data as CSV
   */
  exportChartData(): void {
    const csv = this.generateCSV();
    const link = document.createElement('a');
    link.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    link.download = `weighbridge-data-${this.startDate}-${this.endDate}.csv`;
    link.click();
  }

  private generateCSV(): string {
    let csv = 'Weighbridge Chart Data Export\n';
    csv += `From: ${this.startDate} To: ${this.endDate}\n`;
    csv += `Weighbridge ID: ${this.selectedWbId}\n\n`;

    csv += 'Net Weight Data:\n';
    csv += 'Date/Time,Weight(kg)\n';
    if (this.netWeightChartData.labels && this.netWeightChartData.datasets[0].data) {
      this.netWeightChartData.labels.forEach((label, index) => {
        csv += `${label},${this.netWeightChartData.datasets[0].data[index]}\n`;
      });
    }

    return csv;
  }
}

