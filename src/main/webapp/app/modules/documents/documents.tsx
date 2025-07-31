import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  CardBody,
  CardHeader,
  Table,
  Button,
  Input,
  FormGroup,
  Label,
  Alert,
  Badge,
  Progress,
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from 'reactstrap';
import axios from 'axios';

interface Document {
  id: number;
  title: string;
  filename: string;
  contentType: string;
  fileSize: number;
  status: string;
  summary: string;
  createdDate: string;
}

export const Documents = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [message, setMessage] = useState<{ type: 'success' | 'danger', text: string } | null>(null);
  const [deleteModal, setDeleteModal] = useState<{ show: boolean; documentId?: number }>({ show: false });

  const loadDocuments = useCallback(async () => {
    try {
      const response = await axios.get('/api/documents');
      setDocuments(response.data);
    } catch (error) {
      console.error('Failed to load documents:', error);
      setMessage({ type: 'danger', text: '加载文档列表失败' });
    }
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const maxSize = 50 * 1024 * 1024; // 50MB
      if (file.size > maxSize) {
        setMessage({ type: 'danger', text: '文件大小不能超过50MB' });
        return;
      }
      
      const allowedTypes = [
        'application/pdf',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'text/plain',
        'text/markdown'
      ];
      
      if (!allowedTypes.includes(file.type)) {
        setMessage({ type: 'danger', text: '不支持的文件类型。请上传PDF、Word文档、文本文件或Markdown文件。' });
        return;
      }
      
      setSelectedFile(file);
      setMessage(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage({ type: 'danger', text: '请选择要上传的文件' });
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await axios.post('/api/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / (progressEvent.total || 1));
          setUploadProgress(progress);
        },
      });

      setMessage({ type: 'success', text: '文档上传成功！正在处理中...' });
      setSelectedFile(null);
      setUploadProgress(0);
      
      // Reset file input
      const fileInput = document.getElementById('file-input') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
      
      // Reload documents list
      await loadDocuments();
    } catch (error: any) {
      console.error('Upload failed:', error);
      setMessage({ 
        type: 'danger', 
        text: error.response?.data?.detail || '文档上传失败' 
      });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async (documentId: number) => {
    try {
      await axios.delete(`/api/documents/${documentId}`);
      setMessage({ type: 'success', text: '文档删除成功' });
      setDeleteModal({ show: false });
      await loadDocuments();
    } catch (error) {
      console.error('Delete failed:', error);
      setMessage({ type: 'danger', text: '文档删除失败' });
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PROCESSED': return 'success';
      case 'PROCESSING': return 'warning';
      case 'ERROR': return 'danger';
      default: return 'secondary';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'UPLOADED': return '已上传';
      case 'PROCESSING': return '处理中';
      case 'PROCESSED': return '已处理';
      case 'ERROR': return '处理失败';
      default: return status;
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="container-fluid">
      <h2>文档管理</h2>
      
      {message && (
        <Alert color={message.type} className="mb-4">
          {message.text}
        </Alert>
      )}

      <Card className="mb-4">
        <CardHeader>
          <h5>上传文档</h5>
        </CardHeader>
        <CardBody>
          <FormGroup>
            <Label for="file-input">选择文件</Label>
            <Input
              type="file"
              id="file-input"
              accept=".pdf,.docx,.txt,.md"
              onChange={handleFileSelect}
              disabled={isUploading}
            />
            <small className="form-text text-muted">
              支持的文件格式：PDF、Word文档、文本文件、Markdown。最大文件大小：50MB
            </small>
          </FormGroup>
          
          {selectedFile && (
            <div className="mb-3">
              <strong>已选择文件：</strong> {selectedFile.name} ({formatFileSize(selectedFile.size)})
            </div>
          )}
          
          {isUploading && (
            <div className="mb-3">
              <Progress value={uploadProgress} className="mb-2" />
              <div>上传进度: {uploadProgress}%</div>
            </div>
          )}
          
          <Button
            color="primary"
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
          >
            {isUploading ? '上传中...' : '上传文档'}
          </Button>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <h5>文档列表</h5>
        </CardHeader>
        <CardBody>
          {documents.length === 0 ? (
            <Alert color="info">暂无文档。请上传您的第一个文档开始使用。</Alert>
          ) : (
            <Table responsive>
              <thead>
                <tr>
                  <th>标题</th>
                  <th>文件名</th>
                  <th>大小</th>
                  <th>状态</th>
                  <th>上传时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {documents.map((doc) => (
                  <tr key={doc.id}>
                    <td>{doc.title}</td>
                    <td>{doc.filename}</td>
                    <td>{formatFileSize(doc.fileSize)}</td>
                    <td>
                      <Badge color={getStatusColor(doc.status)}>
                        {getStatusText(doc.status)}
                      </Badge>
                    </td>
                    <td>{new Date(doc.createdDate).toLocaleString('zh-CN')}</td>
                    <td>
                      <Button
                        color="danger"
                        size="sm"
                        onClick={() => setDeleteModal({ show: true, documentId: doc.id })}
                      >
                        删除
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </CardBody>
      </Card>

      <Modal isOpen={deleteModal.show} toggle={() => setDeleteModal({ show: false })}>
        <ModalHeader toggle={() => setDeleteModal({ show: false })}>
          确认删除
        </ModalHeader>
        <ModalBody>
          确定要删除这个文档吗？此操作不可恢复。
        </ModalBody>
        <ModalFooter>
          <Button color="danger" onClick={() => deleteModal.documentId && handleDelete(deleteModal.documentId)}>
            删除
          </Button>
          <Button color="secondary" onClick={() => setDeleteModal({ show: false })}>
            取消
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

export default Documents;