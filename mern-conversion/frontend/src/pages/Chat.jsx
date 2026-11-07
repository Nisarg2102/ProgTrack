import React, { useState, useEffect, useRef } from 'react';
import { io } from 'socket.io-client';

const Chat = () => {
  const [socket, setSocket] = useState(null);
  const [contacts, setContacts] = useState([]);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [activeContact, setActiveContact] = useState(null);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [unreadCounts, setUnreadCounts] = useState({});
  const messagesEndRef = useRef(null);
  const activeContactRef = useRef(activeContact);

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';
  const storedUser = localStorage.getItem('user');
  const currentUser = storedUser
    ? JSON.parse(storedUser)
    : { email: 'user@progtrack.com', fullName: 'Current User', role: 'INTERN' };

  // Keep ref in sync so socket listener always knows who is active
  useEffect(() => {
    activeContactRef.current = activeContact;
  }, [activeContact]);

  // Fetch all registered users to populate directory
  useEffect(() => {
    const fetchUsers = async () => {
      setLoadingContacts(true);
      const token = localStorage.getItem('token');
      try {
        const res = await fetch(`${API_URL}/api/auth/users`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
          const users = await res.json();
          // Exclude the current user so they don't message themselves
          const registeredContacts = users
            .filter((u) => u.email !== currentUser.email)
            .map((u) => ({
              id: u._id,
              name: u.fullName || u.email,
              email: u.email,
              role: u.role || 'MEMBER',
              designation: u.designation || u.role,
              department: u.department || 'General',
              avatar: u.fullName ? u.fullName[0].toUpperCase() : 'U',
              lastLogin: u.currentLoginTime
            }));

          setContacts(registeredContacts);
          if (registeredContacts.length > 0 && !activeContactRef.current) {
            setActiveContact(registeredContacts[0]);
          }
        }
      } catch (err) {
        console.error('Failed to load registered users for chat:', err);
      } finally {
        setLoadingContacts(false);
      }
    };

    fetchUsers();
  }, [currentUser.email, API_URL]);

  // Load private conversation history when activeContact changes
  useEffect(() => {
    if (!activeContact) {
      setMessages([]);
      return;
    }

    // Clear unread count for the opened contact
    setUnreadCounts((prev) => ({ ...prev, [activeContact.email]: 0 }));

    const fetchHistory = async () => {
      setLoadingMessages(true);
      const token = localStorage.getItem('token');
      try {
        const res = await fetch(`${API_URL}/api/chat/conversation/${activeContact.email}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
          const history = await res.json();
          setMessages(
            history.map((m) => ({
              id: m._id,
              text: m.content,
              sender: m.senderEmail === currentUser.email ? 'me' : 'them',
              senderEmail: m.senderEmail,
              time: new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            }))
          );
        }
      } catch (err) {
        console.error('Failed to load chat history:', err);
      } finally {
        setLoadingMessages(false);
      }
    };

    fetchHistory();
  }, [activeContact, API_URL, currentUser.email]);

  // Setup Socket.IO connection
  useEffect(() => {
    const newSocket = io(API_URL, {
      transports: ['websocket', 'polling']
    });

    newSocket.on('connect', () => {
      console.log('[Socket] Connected as:', currentUser.email);
      newSocket.emit('addUser', { userEmail: currentUser.email });
    });

    newSocket.on('messages', (messagePayload) => {
      const current = activeContactRef.current;
      const isForCurrentChat =
        current &&
        ((messagePayload.senderEmail === current.email && messagePayload.recipientEmail === currentUser.email) ||
          (messagePayload.senderEmail === currentUser.email && messagePayload.recipientEmail === current.email));

      if (isForCurrentChat) {
        setMessages((prev) => {
          // Check if this message was already added optimistically by clientMsgId or _id
          const existingIdx = prev.findIndex(
            (m) =>
              (messagePayload.clientMsgId && (m.clientMsgId === messagePayload.clientMsgId || m.id === messagePayload.clientMsgId)) ||
              (messagePayload._id && m.id === messagePayload._id)
          );

          if (existingIdx !== -1) {
            // Replace the optimistic message in-place with confirmed server data (prevents duplicate)
            const updated = [...prev];
            updated[existingIdx] = {
              ...updated[existingIdx],
              id: messagePayload._id,
              clientMsgId: messagePayload.clientMsgId,
              time: new Date(messagePayload.createdAt || Date.now()).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit'
              })
            };
            return updated;
          }

          // New incoming message from the other party
          return [
            ...prev,
            {
              id: messagePayload._id || Date.now(),
              clientMsgId: messagePayload.clientMsgId,
              text: messagePayload.content,
              sender: messagePayload.senderEmail === currentUser.email ? 'me' : 'them',
              senderEmail: messagePayload.senderEmail,
              time: new Date(messagePayload.createdAt || Date.now()).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit'
              })
            }
          ];
        });
      } else if (messagePayload.senderEmail !== currentUser.email) {
        // Increment unread count for the sender
        setUnreadCounts((prev) => ({
          ...prev,
          [messagePayload.senderEmail]: (prev[messagePayload.senderEmail] || 0) + 1
        }));
      }
    });

    setSocket(newSocket);
    return () => newSocket.close();
  }, [API_URL, currentUser.email]);

  // Auto scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || !activeContact) return;

    const messageText = inputMessage.trim();
    const clientMsgId = `c_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
    const optimisticMsg = {
      id: clientMsgId,
      clientMsgId,
      text: messageText,
      sender: 'me',
      senderEmail: currentUser.email,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    // Add exactly once to the state
    setMessages((prev) => [...prev, optimisticMsg]);
    setInputMessage('');

    if (socket && socket.connected) {
      socket.emit('sendMessage', {
        clientMsgId,
        senderEmail: currentUser.email,
        recipientEmail: activeContact.email,
        content: messageText
      });
    } else {
      // Fallback to REST API if socket is offline
      const token = localStorage.getItem('token');
      try {
        const res = await fetch(`${API_URL}/api/chat/send`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({
            clientMsgId,
            recipientEmail: activeContact.email,
            content: messageText
          })
        });
        if (res.ok) {
          const saved = await res.json();
          setMessages((prev) =>
            prev.map((m) => (m.clientMsgId === clientMsgId ? { ...m, id: saved._id } : m))
          );
        }
      } catch (err) {
        console.error('REST chat send fallback failed:', err);
      }
    }
  };

  // Filter contacts by search
  const filteredContacts = contacts.filter((c) => {
    const q = searchTerm.toLowerCase();
    return (
      c.name.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q) ||
      c.role.toLowerCase().includes(q) ||
      c.department.toLowerCase().includes(q)
    );
  });

  const getRoleBadgeClass = (role) => {
    switch (role) {
      case 'CEO':
        return 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border-amber-300 dark:border-amber-800';
      case 'ADMIN':
        return 'bg-red-100 text-red-800 dark:bg-red-950/60 dark:text-red-300 border-red-300 dark:border-red-800';
      case 'LEAD':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300 border-purple-300 dark:border-purple-800';
      case 'INTERN':
      default:
        return 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300 border-blue-300 dark:border-blue-800';
    }
  };

  const getAvatarBg = (role) => {
    switch (role) {
      case 'CEO':
        return 'bg-amber-600';
      case 'ADMIN':
        return 'bg-red-600';
      case 'LEAD':
        return 'bg-purple-600';
      case 'INTERN':
      default:
        return 'bg-blue-600';
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-gray-200 dark:border-slate-700 overflow-hidden">
      {/* ── Left Sidebar: Registered Users Directory ── */}
      <div
        className={`w-full md:w-80 lg:w-96 border-r border-gray-200 dark:border-slate-700 flex flex-col bg-gray-50 dark:bg-slate-900 ${
          activeContact ? 'hidden md:flex' : 'flex'
        }`}
      >
        <div className="p-4 bg-blue-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-lg font-bold flex items-center">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 mr-2"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z"
                  clipRule="evenodd"
                />
              </svg>
              Private Team Chat
            </h2>
            <span className="text-[10px] bg-blue-700 px-2 py-0.5 rounded-full font-semibold">
              {contacts.length} Members
            </span>
          </div>
          <p className="text-xs text-blue-200 mb-3">1-on-1 private messaging for all registered users</p>

          {/* Search bar */}
          <div className="relative">
            <input
              type="text"
              placeholder="Search by name, role, dept..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-blue-700/80 text-white placeholder-blue-300 border border-blue-500/40 rounded-lg py-1.5 pl-8 pr-3 focus:outline-none focus:ring-2 focus:ring-blue-400 text-xs"
            />
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-4 w-4 absolute left-2.5 top-2 text-blue-300"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>

        {/* Directory List */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 dark:divide-slate-800">
          {loadingContacts ? (
            <div className="p-8 text-center text-xs text-gray-500">
              <div className="inline-block animate-spin rounded-full h-5 w-5 border-2 border-blue-600 border-t-transparent mb-2"></div>
              <p>Loading team directory...</p>
            </div>
          ) : filteredContacts.length === 0 ? (
            <div className="p-8 text-center text-xs text-gray-500">
              <p className="font-semibold">No contacts found</p>
              <p className="mt-1 text-[11px] text-gray-400">All registered users will appear here automatically.</p>
            </div>
          ) : (
            filteredContacts.map((contact) => {
              const isSelected = activeContact?.email === contact.email;
              const unread = unreadCounts[contact.email] || 0;
              return (
                <div
                  key={contact.id}
                  onClick={() => setActiveContact(contact)}
                  className={`flex items-center p-3 cursor-pointer transition-colors ${
                    isSelected
                      ? 'bg-blue-50 dark:bg-slate-800/90 border-l-4 border-l-blue-600'
                      : 'hover:bg-gray-100/70 dark:hover:bg-slate-800/50 border-l-4 border-l-transparent'
                  }`}
                >
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm flex-shrink-0 shadow-sm ${getAvatarBg(
                      contact.role
                    )}`}
                  >
                    {contact.avatar}
                  </div>

                  <div className="ml-3 flex-1 overflow-hidden min-w-0">
                    <div className="flex justify-between items-center mb-0.5">
                      <h3 className="font-semibold text-gray-900 dark:text-white truncate text-sm">
                        {contact.name}
                      </h3>
                      <span
                        className={`text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase tracking-wider ${getRoleBadgeClass(
                          contact.role
                        )}`}
                      >
                        {contact.role}
                      </span>
                    </div>

                    <div className="flex justify-between items-center text-xs text-gray-500 dark:text-gray-400">
                      <p className="truncate text-[11px]">
                        {contact.designation} &bull; {contact.department}
                      </p>
                      {unread > 0 && (
                        <span className="ml-2 bg-blue-600 text-white text-[10px] font-bold px-1.5 py-0.2 rounded-full min-w-[18px] text-center">
                          {unread}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Current user footer */}
        <div className="p-3 bg-white dark:bg-slate-900 border-t border-gray-200 dark:border-slate-800 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 truncate">
            <span className="w-2.5 h-2.5 rounded-full bg-green-500 flex-shrink-0 animate-pulse"></span>
            <span className="truncate text-gray-700 dark:text-gray-300 font-medium">
              Signed in as <strong>{currentUser.fullName}</strong>
            </span>
          </div>
          <span className="text-[10px] uppercase font-bold text-blue-600 dark:text-blue-400">
            {currentUser.role}
          </span>
        </div>
      </div>

      {/* ── Right Panel: Private 1-on-1 Conversation ── */}
      <div className={`flex-1 flex flex-col ${!activeContact ? 'hidden md:flex' : 'flex'}`}>
        {!activeContact ? (
          <div className="flex-1 flex flex-col items-center justify-center bg-gray-50 dark:bg-slate-900 text-gray-500 dark:text-gray-400 p-6 text-center">
            <div className="w-16 h-16 rounded-full bg-blue-50 dark:bg-slate-800 flex items-center justify-center text-blue-600 mb-3 shadow-inner">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-1">Select a colleague</h3>
            <p className="text-xs max-w-sm">
              Click any team member from the directory (CEO, Team Lead, Intern, Admin) to start an encrypted private chat.
            </p>
          </div>
        ) : (
          <>
            {/* Conversation Header */}
            <div className="px-5 py-3 border-b border-gray-200 dark:border-slate-700 flex items-center justify-between bg-white dark:bg-slate-900 z-10 shadow-sm">
              <div className="flex items-center">
                <button
                  onClick={() => setActiveContact(null)}
                  className="md:hidden mr-3 p-1.5 text-gray-500 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-lg"
                  title="Back to directory"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                  </svg>
                </button>

                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm flex-shrink-0 mr-3 shadow-sm ${getAvatarBg(
                    activeContact.role
                  )}`}
                >
                  {activeContact.avatar}
                </div>

                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-gray-900 dark:text-white text-sm">{activeContact.name}</h3>
                    <span
                      className={`text-[9px] font-bold px-1.5 py-0.5 rounded border uppercase tracking-wider ${getRoleBadgeClass(
                        activeContact.role
                      )}`}
                    >
                      {activeContact.role}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {activeContact.email} &bull; <span className="text-green-600 dark:text-green-400 font-medium">Private 1-on-1</span>
                  </p>
                </div>
              </div>

              <div className="text-right hidden sm:block">
                <span className="text-[11px] text-gray-400">
                  {activeContact.department} Department
                </span>
              </div>
            </div>

            {/* Message Stream */}
            <div className="flex-1 overflow-y-auto p-4 sm:p-6 bg-gray-50/60 dark:bg-slate-900/40 space-y-3">
              {loadingMessages ? (
                <div className="text-center py-12 text-gray-500 text-xs">
                  <div className="inline-block animate-spin rounded-full h-5 w-5 border-2 border-blue-600 border-t-transparent mb-2"></div>
                  <p>Loading private conversation...</p>
                </div>
              ) : messages.length === 0 ? (
                <div className="text-center py-16 text-gray-400 text-xs max-w-sm mx-auto">
                  <div className="w-12 h-12 rounded-full bg-blue-50 dark:bg-slate-800 flex items-center justify-center text-blue-500 mx-auto mb-2">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </div>
                  <p className="font-semibold text-gray-600 dark:text-gray-300">Strictly Private Conversation</p>
                  <p className="mt-1 text-[11px]">
                    Messages exchanged here between you and <strong>{activeContact.name}</strong> are completely private and only visible to you two.
                  </p>
                </div>
              ) : (
                messages.map((msg) => {
                  const isMe = msg.sender === 'me';
                  return (
                    <div
                      key={msg.id}
                      className={`flex flex-col max-w-[80%] sm:max-w-[70%] ${
                        isMe ? 'ml-auto items-end' : 'mr-auto items-start'
                      }`}
                    >
                      <div
                        className={`px-4 py-2.5 rounded-2xl shadow-sm text-sm break-words ${
                          isMe
                            ? 'bg-blue-600 text-white rounded-tr-sm'
                            : 'bg-white dark:bg-slate-800 text-gray-800 dark:text-gray-100 border border-gray-200 dark:border-slate-700 rounded-tl-sm'
                        }`}
                      >
                        {msg.text}
                      </div>
                      <div className="text-[10px] text-gray-400 mt-1 px-1 flex items-center gap-1">
                        {!isMe && <span className="font-semibold text-blue-600">{activeContact.name}</span>}
                        <span>{msg.time}</span>
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Form */}
            <div className="p-3 sm:p-4 bg-white dark:bg-slate-900 border-t border-gray-200 dark:border-slate-700">
              <form onSubmit={sendMessage} className="flex gap-2 items-center">
                <input
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder={`Send private message to ${activeContact.name} (${activeContact.role})...`}
                  className="flex-1 bg-gray-100 dark:bg-slate-800 border-0 rounded-full px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none dark:text-white"
                />
                <button
                  type="submit"
                  disabled={!inputMessage.trim()}
                  className="w-10 h-10 rounded-full bg-blue-600 text-white flex items-center justify-center disabled:opacity-50 hover:bg-blue-700 transition-colors shadow-sm flex-shrink-0"
                  title="Send message"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 ml-0.5" viewBox="0 0 20 20" fill="currentColor">
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-8.707l-3-3a1 1 0 00-1.414 1.414L10.586 9H7a1 1 0 100 2h3.586l-1.293 1.293a1 1 0 101.414 1.414l3-3a1 1 0 00-1.414z"
                      clipRule="evenodd"
                    />
                  </svg>
                </button>
              </form>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default Chat;
