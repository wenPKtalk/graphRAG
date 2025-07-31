import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Card, CardBody, CardHeader, Input, Button, Alert, Badge, Collapse, Spinner, Form, FormGroup } from 'reactstrap';
import axios from 'axios';

interface ContextChunk {
  id: number;
  content: string;
  documentTitle: string;
  chunkIndex: number;
}

interface RelatedEntity {
  id: number;
  name: string;
  type: string;
  description: string;
}

interface QueryResponse {
  answer: string;
  contextChunks: ContextChunk[];
  relatedEntities: RelatedEntity[];
  sessionId: string;
}

interface ChatMessage {
  id: string;
  question: string;
  answer: string;
  timestamp: Date;
  contextChunks: ContextChunk[];
  relatedEntities: RelatedEntity[];
  isLoading?: boolean;
}

interface QueryHistory {
  id: number;
  question: string;
  answer: string;
  createdDate: string;
}

export const Chat = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [currentQuestion, setCurrentQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [queryHistory, setQueryHistory] = useState<QueryHistory[]>([]);
  const [expandedContexts, setExpandedContexts] = useState<Set<string>>(new Set());
  const [expandedEntities, setExpandedEntities] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const setInputRef = useCallback((node: HTMLInputElement | null) => {
    inputRef.current = node;
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    // Generate session ID on component mount
    setSessionId(generateSessionId());
  }, []);

  useEffect(() => {
    if (sessionId) {
      loadQueryHistory();
    }
  }, [sessionId]);

  const generateSessionId = () => {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  };

  const loadQueryHistory = async () => {
    try {
      const response = await axios.get(`/api/query/history?sessionId=${sessionId}&limit=5`);
      setQueryHistory(response.data);
    } catch (error) {
      console.error('Failed to load query history:', error);
    }
  };

  const getSuggestions = useCallback(async (query: string) => {
    if (query.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    try {
      const response = await axios.get(`/api/query/suggestions?q=${encodeURIComponent(query)}`);
      setSuggestions(response.data);
      setShowSuggestions(response.data.length > 0);
    } catch (err) {
      console.error('Failed to get suggestions:', err);
      setSuggestions([]);
      setShowSuggestions(false);
    }
  }, []);

  const handleQuestionChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setCurrentQuestion(value);
    getSuggestions(value);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentQuestion.trim() || isLoading) return;

    const question = currentQuestion.trim();
    const messageId = generateMessageId();

    // Add loading message
    const loadingMessage: ChatMessage = {
      id: messageId,
      question,
      answer: '',
      timestamp: new Date(),
      contextChunks: [],
      relatedEntities: [],
      isLoading: true,
    };

    setMessages(prev => [...prev, loadingMessage]);
    setCurrentQuestion('');
    setShowSuggestions(false);
    setIsLoading(true);
    setError(null);

    try {
      const response = await axios.post<QueryResponse>('/api/query', {
        question,
        sessionId,
      });

      const completedMessage: ChatMessage = {
        id: messageId,
        question,
        answer: response.data.answer,
        timestamp: new Date(),
        contextChunks: response.data.contextChunks,
        relatedEntities: response.data.relatedEntities,
        isLoading: false,
      };

      setMessages(prev => prev.map(msg => (msg.id === messageId ? completedMessage : msg)));

      // Refresh query history
      await loadQueryHistory();
    } catch (err: any) {
      console.error('Query failed:', err);
      setError(err.response?.data?.detail || '查询失败，请稍后重试。');

      // Remove loading message on error
      setMessages(prev => prev.filter(msg => msg.id !== messageId));
    } finally {
      setIsLoading(false);
    }
  };

  const generateMessageId = () => {
    return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  };

  const handleSuggestionClick = (suggestion: string) => {
    setCurrentQuestion(suggestion);
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  const toggleContextExpanded = (messageId: string) => {
    setExpandedContexts(prev => {
      const newSet = new Set(prev);
      if (newSet.has(messageId)) {
        newSet.delete(messageId);
      } else {
        newSet.add(messageId);
      }
      return newSet;
    });
  };

  const toggleEntitiesExpanded = (messageId: string) => {
    setExpandedEntities(prev => {
      const newSet = new Set(prev);
      if (newSet.has(messageId)) {
        newSet.delete(messageId);
      } else {
        newSet.add(messageId);
      }
      return newSet;
    });
  };

  const getEntityTypeColor = (type: string) => {
    const colors: { [key: string]: string } = {
      PERSON: 'primary',
      ORGANIZATION: 'success',
      LOCATION: 'info',
      CONCEPT: 'warning',
      PRODUCT: 'secondary',
      EVENT: 'danger',
    };
    return colors[type] || 'light';
  };

  return (
    <div className="container-fluid">
      <div className="row">
        <div className="col-md-9">
          <Card className="chat-card">
            <CardHeader>
              <h5>智能问答</h5>
              <small className="text-muted">基于您上传的文档内容进行问答</small>
            </CardHeader>
            <CardBody>
              {error && (
                <Alert color="danger" className="mb-3">
                  {error}
                </Alert>
              )}

              <div className="chat-messages" style={{ maxHeight: '400px', overflowY: 'auto', marginBottom: '20px' }}>
                {messages.length === 0 && (
                  <div className="text-center text-muted py-4">
                    <p>欢迎使用智能问答系统！</p>
                    <p>请在下方输入您的问题，我会基于您上传的文档内容来回答。</p>
                  </div>
                )}

                {messages.map((message) => (
                  <div key={message.id} className="message-pair mb-4">
                    <div className="question-bubble bg-primary text-white p-3 rounded mb-2">
                      <strong>问：</strong> {message.question}
                      <small className="d-block mt-1 opacity-75">
                        {message.timestamp.toLocaleString('zh-CN')}
                      </small>
                    </div>

                    <div className="answer-bubble bg-light p-3 rounded">
                      {message.isLoading ? (
                        <div className="text-center">
                          <Spinner size="sm" /> 思考中...
                        </div>
                      ) : (
                        <>
                          <strong>答：</strong>
                          <div className="mt-2" style={{ whiteSpace: 'pre-wrap' }}>
                            {message.answer}
                          </div>

                          {message.contextChunks.length > 0 && (
                            <div className="mt-3">
                              <Button
                                color="link"
                                size="sm"
                                onClick={() => toggleContextExpanded(message.id)}
                                className="p-0"
                              >
                                参考文档片段 ({message.contextChunks.length})
                                <i className={`fa fa-chevron-${expandedContexts.has(message.id) ? 'up' : 'down'} ms-1`}></i>
                              </Button>
                              <Collapse isOpen={expandedContexts.has(message.id)}>
                                <div className="mt-2">
                                  {message.contextChunks.map((chunk, index) => (
                                    <div key={chunk.id} className="context-chunk border-start border-primary ps-3 mb-2">
                                      <small className="text-muted">
                                        文档: {chunk.documentTitle} | 片段 {chunk.chunkIndex + 1}
                                      </small>
                                      <div className="small mt-1">
                                        {chunk.content.length > 200
                                          ? chunk.content.substring(0, 200) + '...'
                                          : chunk.content}
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              </Collapse>
                            </div>
                          )}

                          {message.relatedEntities.length > 0 && (
                            <div className="mt-3">
                              <Button
                                color="link"
                                size="sm"
                                onClick={() => toggleEntitiesExpanded(message.id)}
                                className="p-0"
                              >
                                相关实体 ({message.relatedEntities.length})
                                <i className={`fa fa-chevron-${expandedEntities.has(message.id) ? 'up' : 'down'} ms-1`}></i>
                              </Button>
                              <Collapse isOpen={expandedEntities.has(message.id)}>
                                <div className="mt-2">
                                  {message.relatedEntities.map((entity) => (
                                    <Badge
                                      key={entity.id}
                                      color={getEntityTypeColor(entity.type)}
                                      className="me-2 mb-1"
                                      title={entity.description}
                                    >
                                      {entity.name} ({entity.type})
                                    </Badge>
                                  ))}
                                </div>
                              </Collapse>
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>

              <Form onSubmit={handleSubmit} className="position-relative">
                <FormGroup className="mb-0">
                  <div className="d-flex">
                    <Input
                      innerRef={setInputRef}
                      type="text"
                      placeholder="请输入您的问题..."
                      value={currentQuestion}
                      onChange={handleQuestionChange}
                      disabled={isLoading}
                      autoComplete="off"
                    />
                    <Button
                      type="submit"
                      color="primary"
                      disabled={!currentQuestion.trim() || isLoading}
                      className="ms-2"
                    >
                      {isLoading ? <Spinner size="sm" /> : '发送'}
                    </Button>
                  </div>
                </FormGroup>

                {showSuggestions && suggestions.length > 0 && (
                  <div className="suggestions-dropdown position-absolute w-100 bg-white border rounded shadow-sm mt-1" style={{ zIndex: 1000 }}>
                    {suggestions.map((suggestion, index) => (
                      <div
                        key={index}
                        className="suggestion-item p-2 border-bottom cursor-pointer"
                        onClick={() => handleSuggestionClick(suggestion)}
                        style={{ cursor: 'pointer' }}
                        onMouseEnter={(e) => e.currentTarget.classList.add('bg-light')}
                        onMouseLeave={(e) => e.currentTarget.classList.remove('bg-light')}
                      >
                        {suggestion}
                      </div>
                    ))}
                  </div>
                )}
              </Form>
            </CardBody>
          </Card>
        </div>

        <div className="col-md-3">
          <Card>
            <CardHeader>
              <h6>最近问题</h6>
            </CardHeader>
            <CardBody>
              {queryHistory.length === 0 ? (
                <small className="text-muted">暂无历史记录</small>
              ) : (
                <div>
                  {queryHistory.map((query) => (
                    <div
                      key={query.id}
                      className="history-item p-2 border-bottom cursor-pointer small"
                      onClick={() => setCurrentQuestion(query.question)}
                      style={{ cursor: 'pointer' }}
                      onMouseEnter={(e) => e.currentTarget.classList.add('bg-light')}
                      onMouseLeave={(e) => e.currentTarget.classList.remove('bg-light')}
                    >
                      <div className="text-truncate">{query.question}</div>
                      <small className="text-muted">
                        {new Date(query.createdDate).toLocaleDateString('zh-CN')}
                      </small>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Chat;
