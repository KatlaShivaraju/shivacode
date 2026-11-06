export interface UserProfile {
  id: number;
  username: string;
  email: string;
  memberSince: string;
  totalSubmissions: number;
  acceptedSubmissions: number;
  solvedProblems: number;
  acceptanceRate: number;
}