import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';

import { Alert, Col, Row, Card, CardBody, CardHeader, Button } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <Row>
      <Col md="12">
        <h1 className="display-4">欢迎使用 GraphRAG 知识问答系统</h1>
        <p className="lead">基于知识图谱的智能文档问答平台</p>
        
        {account?.login ? (
          <div>
            <Alert color="success">欢迎您，{account.login}！</Alert>
            
            <Row className="mt-4">
              <Col md="6">
                <Card>
                  <CardHeader>
                    <h5><i className="fa fa-upload me-2"></i>文档管理</h5>
                  </CardHeader>
                  <CardBody>
                    <p>上传和管理您的文档，系统会自动处理并构建知识图谱。</p>
                    <ul>
                      <li>支持 PDF、Word、文本等多种格式</li>
                      <li>自动提取实体和关系</li>
                      <li>生成文档摘要和向量嵌入</li>
                    </ul>
                    <Link to="/documents">
                      <Button color="primary">进入文档管理</Button>
                    </Link>
                  </CardBody>
                </Card>
              </Col>
              
              <Col md="6">
                <Card>
                  <CardHeader>
                    <h5><i className="fa fa-comments me-2"></i>智能问答</h5>
                  </CardHeader>
                  <CardBody>
                    <p>基于上传的文档内容进行智能问答，获得准确的信息检索。</p>
                    <ul>
                      <li>语义搜索和相似度匹配</li>
                      <li>显示答案来源和相关实体</li>
                      <li>问题建议和历史记录</li>
                    </ul>
                    <Link to="/chat">
                      <Button color="success">开始问答</Button>
                    </Link>
                  </CardBody>
                </Card>
              </Col>
            </Row>
            
            <Card className="mt-4">
              <CardHeader>
                <h5><i className="fa fa-info-circle me-2"></i>系统特性</h5>
              </CardHeader>
              <CardBody>
                <Row>
                  <Col md="4">
                    <h6><i className="fa fa-brain me-2"></i>知识图谱</h6>
                    <p className="small">自动构建实体关系网络，深度理解文档内容结构。</p>
                  </Col>
                  <Col md="4">
                    <h6><i className="fa fa-search me-2"></i>语义检索</h6>
                    <p className="small">基于向量嵌入的语义搜索，准确理解用户意图。</p>
                  </Col>
                  <Col md="4">
                    <h6><i className="fa fa-robot me-2"></i>智能对话</h6>
                    <p className="small">结合大语言模型，提供自然流畅的问答体验。</p>
                  </Col>
                </Row>
              </CardBody>
            </Card>
          </div>
        ) : (
          <div>
            <Alert color="warning">
              请先
              <span>&nbsp;</span>
              <Link to="/login" className="alert-link">
                登录
              </Link>
              &nbsp;以使用系统功能。
              <br />- 管理员账户 (用户名: admin, 密码: admin) 
              <br />- 普通用户账户 (用户名: user, 密码: user)
            </Alert>
            
            <Card className="mt-4">
              <CardHeader>
                <h5>系统介绍</h5>
              </CardHeader>
              <CardBody>
                <p>GraphRAG 是一个基于知识图谱的检索增强生成（RAG）系统，具有以下特点：</p>
                <ul>
                  <li><strong>智能文档处理</strong>：自动解析多种格式文档，提取关键信息</li>
                  <li><strong>知识图谱构建</strong>：自动识别实体和关系，构建结构化知识网络</li>
                  <li><strong>语义搜索</strong>：基于向量嵌入的相似度匹配，精准检索相关内容</li>
                  <li><strong>智能问答</strong>：结合大语言模型，提供准确、有据可查的答案</li>
                </ul>
              </CardBody>
            </Card>
          </div>
        )}
      </Col>
    </Row>
  );
};

export default Home;
