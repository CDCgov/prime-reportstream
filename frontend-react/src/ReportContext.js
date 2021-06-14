
import React, { createContext, useState } from "react";
import PropTypes from "prop-types";

export const ReportContext = createContext({});

export const Provider = props => {
  // Initial values are obtained from the props
  const {
    users: initialUsers,
    selectedUser: initialSelectedUsers,
    children
  } = props;

  // Use State to keep the values
  const [users, setUsers] = useState(initialUsers);
  const [selectedUser, setSelectedUser] = useState(initialSelectedUsers);

  const addNewUser = userName => {
    const newUser = { id: new Date().getTime().toString(), name: userName };
    setUsers(users.concat([newUser]));
  };

  // Make the context object:
  const usersContext = {
    users,
    setUsers,
    selectedUser,
    setSelectedUser,
    addNewUser
  };

  // pass the value in provider and return
  return <ReportContext.Provider value={usersContext}>{children}</ReportContext.Provider>;
};

export const { Consumer } = ReportContext;

Provider.propTypes = {
  users: PropTypes.array,
  selectedUser: PropTypes.object
};

Provider.defaultProps = {
  users: [],
  selectedUser: {}
};