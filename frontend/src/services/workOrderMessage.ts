import http from '../utils/api';
import { ChatMessage } from '../hooks/useWorkOrderChat';

export interface SendMessageRequest {
  workOrderId: number;
  messageType: 'TEXT' | 'VOICE' | 'IMAGE' | 'VIDEO' | 'DOCUMENT' | 'SYSTEM';
  content?: string;
  fileId?: number;
  parentMessageId?: number;
}

export interface UpdateMessageRequest {
  content?: string;
  deleted?: boolean;
}

const workOrderMessageService = {
  getMessages: (workOrderId: number) => {
    return http.get<ChatMessage[]>(`/work-order-messages/work-order/${workOrderId}`);
  },

  sendMessage: (data: SendMessageRequest) => {
    return http.post<ChatMessage>('/work-order-messages', data);
  },

  updateMessage: (messageId: number, data: UpdateMessageRequest) => {
    return http.patch<ChatMessage>(`/work-order-messages/${messageId}`, data);
  },

  markAsRead: (messageId: number) => {
    return http.post(`/work-order-messages/${messageId}/read`, {});
  },

  markAllAsRead: (workOrderId: number) => {
    return http.post(`/work-order-messages/work-order/${workOrderId}/read-all`, {});
  },

  getUnreadCount: (workOrderId: number) => {
    return http.get<number>(`/work-order-messages/work-order/${workOrderId}/unread-count`);
  },

  toggleReaction: (messageId: number, reaction: string) => {
    return http.post(`/work-order-messages/${messageId}/reaction?reaction=${reaction}`, {});
  },
};

export default workOrderMessageService;
