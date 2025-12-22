import { useEffect, useState, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAPIUrl } from '../utils/api';

export interface ChatMessage {
  id: number;
  workOrderId: number;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  };
  messageType: 'TEXT' | 'VOICE' | 'IMAGE' | 'VIDEO' | 'DOCUMENT' | 'SYSTEM';
  content?: string;
  file?: {
    id: number;
    name: string;
    path: string;
  };
  parentMessageId?: number;
  edited: boolean;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
  reactions: Array<{
    reaction: string;
    count: number;
    users: Array<{ id: number; firstName: string; lastName: string }>;
    currentUserReacted: boolean;
  }>;
  readBy: Array<{ id: number; firstName: string; lastName: string }>;
  readByCurrentUser: boolean;
}

export interface WebSocketMessage {
  type: 'NEW_MESSAGE' | 'MESSAGE_UPDATED' | 'MESSAGE_DELETED' | 'MESSAGE_READ' | 'REACTION_TOGGLED';
  workOrderId: number;
  message?: ChatMessage;
  messageId?: number;
  userId?: number;
  reaction?: string;
}

export interface TypingNotification {
  userId: number;
  userName: string;
  isTyping: boolean;
}

export const useWorkOrderChat = (workOrderId: number) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map());
  const clientRef = useRef<Client | null>(null);
  const typingTimeoutRef = useRef<Map<number, NodeJS.Timeout>>(new Map());

  // Initialize WebSocket connection
  useEffect(() => {
    const apiUrl = getAPIUrl();
    const wsUrl = `${apiUrl}/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('WebSocket connected');
      setIsConnected(true);

      // Subscribe to work order messages
      client.subscribe(`/topic/work-order/${workOrderId}/messages`, (message) => {
        const data: WebSocketMessage = JSON.parse(message.body);
        handleWebSocketMessage(data);
      });

      // Subscribe to typing indicators
      client.subscribe(`/topic/work-order/${workOrderId}/typing`, (message) => {
        const data: TypingNotification = JSON.parse(message.body);
        handleTypingNotification(data);
      });
    };

    client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [workOrderId]);

  const handleWebSocketMessage = useCallback((data: WebSocketMessage) => {
    switch (data.type) {
      case 'NEW_MESSAGE':
        if (data.message) {
          setMessages((prev) => [...prev, data.message!]);
        }
        break;
      case 'MESSAGE_UPDATED':
        if (data.message) {
          setMessages((prev) =>
            prev.map((msg) => (msg.id === data.message!.id ? data.message! : msg))
          );
        }
        break;
      case 'MESSAGE_DELETED':
        if (data.messageId) {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === data.messageId ? { ...msg, deleted: true } : msg
            )
          );
        }
        break;
      case 'MESSAGE_READ':
        if (data.messageId && data.userId) {
          setMessages((prev) =>
            prev.map((msg) => {
              if (msg.id === data.messageId) {
                return {
                  ...msg,
                  readBy: [...msg.readBy, { id: data.userId!, firstName: '', lastName: '' }],
                };
              }
              return msg;
            })
          );
        }
        break;
      case 'REACTION_TOGGLED':
        // Refresh the specific message to get updated reactions
        if (data.messageId) {
          // This would typically trigger a refresh of the message
          console.log('Reaction toggled for message', data.messageId);
        }
        break;
    }
  }, []);

  const handleTypingNotification = useCallback((data: TypingNotification) => {
    setTypingUsers((prev) => {
      const newMap = new Map(prev);
      
      if (data.isTyping) {
        newMap.set(data.userId, data.userName);
        
        // Clear existing timeout
        const existingTimeout = typingTimeoutRef.current.get(data.userId);
        if (existingTimeout) {
          clearTimeout(existingTimeout);
        }
        
        // Set new timeout to remove typing indicator after 3 seconds
        const timeout = setTimeout(() => {
          setTypingUsers((prev) => {
            const newMap = new Map(prev);
            newMap.delete(data.userId);
            return newMap;
          });
        }, 3000);
        
        typingTimeoutRef.current.set(data.userId, timeout);
      } else {
        newMap.delete(data.userId);
      }
      
      return newMap;
    });
  }, []);

  const sendTypingIndicator = useCallback((userId: number, userName: string, isTyping: boolean) => {
    if (clientRef.current && isConnected) {
      clientRef.current.publish({
        destination: `/app/work-order/${workOrderId}/typing`,
        body: JSON.stringify({ userId, userName, typing: isTyping }),
      });
    }
  }, [workOrderId, isConnected]);

  return {
    messages,
    setMessages,
    unreadCount,
    setUnreadCount,
    isConnected,
    typingUsers: Array.from(typingUsers.values()),
    sendTypingIndicator,
  };
};
