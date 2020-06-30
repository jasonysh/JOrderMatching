package jfixgateway;

public class FIXServerConfiguration {
    //protected static final int DEFAULT_TCP_PENDING_CONNECTION_SIZE = 32;
    //protected static final int DEFAULT_SOCKET_OPTION_SEND_BUFFER_SIZE = 102400;
    //protected static final int DEFAULT_SOCKET_OPTION_RECV_BUFFER_SIZE = 102400;
    //protected static final int DEFAULT_POLL_TIMEOUT_MICROSECONDS = 1000;
    //protected static final int DEFAULT_POLL_MAX_EVENTS = 1024;
    
    //public boolean m_tcpDisableNagle;
    //public boolean m_tcpQuickAck;
    //public int m_tcpPendingConnectionSize;
    //public int m_tcpSocketOptionSendBufferSize;
    //public int m_tcpSocketOptionReceiveBufferSize;
    //public long m_tcpPollTimeoutMicroseconds;
    //public int m_tcpPollMaxEvents;
    // Reactor thread
    //public int m_reactorThreadCpuId;
    //public int m_reactorThreadStackSize;
    // FIX
    private boolean isFixSeqNumValidationEnable;
    private int backlog;
    private int bufferSize;
    private String fixCompId;
    private String fixAddress;
    private int fixReceiveCacheSize;
    private int fixPort;
    
    public FIXServerConfiguration()
    {
        // TCP
        //m_tcpDisableNagle = true;
        //m_tcpQuickAck = true;
        //m_tcpPendingConnectionSize = DEFAULT_TCP_PENDING_CONNECTION_SIZE;
        //m_tcpSocketOptionSendBufferSize = DEFAULT_SOCKET_OPTION_SEND_BUFFER_SIZE;
        //m_tcpSocketOptionReceiveBufferSize = DEFAULT_SOCKET_OPTION_RECV_BUFFER_SIZE;
        //m_tcpPollTimeoutMicroseconds = DEFAULT_POLL_TIMEOUT_MICROSECONDS;
        //m_tcpPollMaxEvents = DEFAULT_POLL_MAX_EVENTS;
        // Reactor thread
        //m_reactorThreadCpuId = -1;
        //m_reactorThreadStackSize = 0;
        // FIX
        isFixSeqNumValidationEnable = true;
        fixReceiveCacheSize = 0;
        backlog = 10;
        fixCompId = "TS";
        bufferSize = 1024;
    }

    public boolean isFixSeqNumValidation() {
      return isFixSeqNumValidationEnable;
    }

    public void setFixSeqNumValidation(boolean fixSeqNumValidation) {
      this.isFixSeqNumValidationEnable = fixSeqNumValidation;
    }

    public String getFixCompId() {
      return fixCompId;
    }

    public void setFixCompId(String fixCompId) {
      this.fixCompId = fixCompId;
    }

    public String getFixAddress() {
      return fixAddress;
    }

    public void setFixAddress(String fixAddress) {
      this.fixAddress = fixAddress;
    }

    public int getFixReceiveCacheSize() {
      return fixReceiveCacheSize;
    }

    public int getBacklog() {
      return backlog;
    }
    
    public int getBufferSize() {
      return bufferSize;
    }

    public void setFixReceiveCacheSize(int fixReceiveCacheSize) {
      this.fixReceiveCacheSize = fixReceiveCacheSize;
    }

    public int getFixPort() {
      return fixPort;
    }

    public void setFixPort(int fixPort) {
      this.fixPort = fixPort;
    }
}
