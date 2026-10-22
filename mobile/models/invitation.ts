export interface InvitationDelivery {
  email: string;
  invitationLink: string;
  expiresAt: string;
}

export interface InvitationResponse {
  success: boolean;
  message: string;
  invitations: InvitationDelivery[];
}
