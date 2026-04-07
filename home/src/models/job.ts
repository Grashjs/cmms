export type SalaryRangeType =
  | '700_1200'
  | '1200_3000'
  | '3000_5000'
  | '5000_above';
export type EmploymentType =
  | 'full_time'
  | 'part_time'
  | 'remote'
  | 'internship'
  | 'freelance';
export type SeniorityType = 'student' | 'entry' | 'mid' | 'director' | 'vp';

export interface Job {
  id: string;
  company_logo: string;
  company_name: string;
  title: string;
  tags: string[];
  location: string;
  salaryRange: SalaryRangeType;
  employment: EmploymentType;
  seniority: SeniorityType;
  postedDate: number;
}
