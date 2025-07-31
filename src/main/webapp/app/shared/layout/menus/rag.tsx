import React from 'react';
import { DropdownItem } from 'reactstrap';
import { NavDropdown } from './menu-components';
import { NavLink as Link } from 'react-router-dom';

export const RAGMenu = () => (
  <NavDropdown icon="fa-brain" name="知识问答" id="rag-menu" data-cy="rag">
    <DropdownItem tag={Link} to="/documents" data-cy="documents">
      <i className="fa fa-upload" /> 文档管理
    </DropdownItem>
    <DropdownItem tag={Link} to="/chat" data-cy="chat">
      <i className="fa fa-comments" /> 智能问答
    </DropdownItem>
  </NavDropdown>
);