#include "network.h"

/* Network Connections Module */

volatile int ip6to4 = 0;
net_info* cur_netinfo = (void *) 0;

int work_net_count = 0;
void *work_net_list = (void *) 0;

int cur_net_count = 0;
void *cur_net_list = (void *) 0;

void net_list_add(char *new_protocol, void *new_localip, unsigned new_localport,
				 void *new_remoteip, unsigned new_remoteport, unsigned new_status,
				 unsigned new_uid, int flag)
{
	// create new node
	net_info* new_node = (net_info *) malloc(sizeof(net_info));

	new_node->tcp6flag = flag;

	strncpy(new_node->protocol, new_protocol, sizeof(new_node->protocol));

	if(flag == 0)
		memcpy(&new_node->localip, new_localip, sizeof(struct in_addr));
	else
		memcpy(&new_node->localip6, new_localip, sizeof(struct in6_addr));

	new_node->localport = new_localport;

	if(flag == 0)
		memcpy(&new_node->remoteip, new_remoteip, sizeof(struct in_addr));
	else
		memcpy(&new_node->remoteip6, new_remoteip, sizeof(struct in6_addr));

	new_node->remoteport = new_remoteport;

	new_node->status = new_status;

	new_node->uid = new_uid;

	new_node->next = (void *) 0;

	// find node
	if(work_net_list != (void *) 0)
	{
		net_info *end = work_net_list;
		while(end->next != (void *) 0)
			end = end->next;

		end->next = new_node;
	}
	else
	{
		work_net_list = new_node;
	}

	work_net_count++;
}

void net_list_empty()
{
	if(work_net_list == (void *) 0)
		return;

	// reset
	net_info *old_node = (net_info *) work_net_list;
	work_net_list = (void *) 0;

	// release memory
	net_info *next_node = (void *) 0;
	while(old_node->next != (void *) 0)
	{
		next_node = (net_info *) old_node->next;
		free(old_node);

		old_node = next_node;
	}
	free(old_node);

	work_net_count = 0;

	return;
}

void net_set_ip6to4(int value)
{
	ip6to4 = value;
}

char *net_state2str(unsigned state)
{
    switch(state)
    {
    case 0x1: return "ESTABLISHED";
    case 0x2: return "SYN_SENT";
    case 0x3: return "SYN_RECV";
    case 0x4: return "FIN_WAIT1";
    case 0x5: return "FIN_WAIT2";
    case 0x6: return "TIME_WAIT";
    case 0x7: return "CLOSE";
    case 0x8: return "CLOSE_WAIT";
    case 0x9: return "LAST_ACK";
    case 0xA: return "LISTEN";
    case 0xB: return "CLOSING";
    default: return "UNKNOWN";

    }
}

void net_addr2str_ipv4(struct in_addr *addr, char *buf)
{
	snprintf(buf, 64, "%d.%d.%d.%d",
             ((char *) &addr->s_addr)[0], ((char *) &addr->s_addr)[1],
             ((char *) &addr->s_addr)[2], ((char *) &addr->s_addr)[3]);
}

void net_addr2str_ipv6(struct in6_addr *addr, char *buf)
{
	if(ip6to4 == 1)
	{
		snprintf(buf, BUFFERSIZE, "%d.%d.%d.%d",
				addr->in6_u.u6_addr8[15], addr->in6_u.u6_addr8[14],
				addr->in6_u.u6_addr8[13], addr->in6_u.u6_addr8[12]);
	}
	else if(IN6_IS_ADDR_UNSPECIFIED(addr))
	{
		strcpy(buf, "");
	}
	else if(IN6_IS_ADDR_LOOPBACK(addr))
	{
		snprintf(buf, BUFFERSIZE, "::1");
    }
	else if(IN6_IS_ADDR_V4COMPAT(addr))
	{
		snprintf(buf, BUFFERSIZE, "::%d.%d.%d.%d",
				addr->in6_u.u6_addr8[15], addr->in6_u.u6_addr8[14],
				addr->in6_u.u6_addr8[13], addr->in6_u.u6_addr8[12]);
	}
	else if(addr->in6_u.u6_addr16[0] == 0x0 && addr->in6_u.u6_addr16[1] == 0x0 &&
			addr->in6_u.u6_addr16[2] == 0x0 && addr->in6_u.u6_addr16[3] == 0x0 &&
			addr->in6_u.u6_addr16[4] == 0xFFFF && addr->in6_u.u6_addr16[5] == 0x0 )
	{
		snprintf(buf, BUFFERSIZE, "ffff:%d.%d.%d.%d",
				addr->in6_u.u6_addr8[15], addr->in6_u.u6_addr8[14],
				addr->in6_u.u6_addr8[13], addr->in6_u.u6_addr8[12]);
	}
	else
	{
		char ipv6buf[256];
		int i;

		buf[0] = 0;
		ipv6buf[0] = 0;
		for(i=0; i < 8;i++)
		{
			ipv6buf[0] = 0;

			if(addr->in6_u.u6_addr16[i] == 0x0)
			{
				if(strlen(buf) < 2)
					strcpy(ipv6buf, ":");
				else if( strcmp( buf+(strlen(buf)-2), "::") != 0)
					strcpy(ipv6buf, ":");
			}
			else
			{
				if(strlen(buf) == 0 || buf[strlen(buf)-1] == ':')
				{
					snprintf(ipv6buf, BUFFERSIZE, "%x%x",
							addr->in6_u.u6_addr8[i*2] ,addr->in6_u.u6_addr8[i*2+1]);
				}
				else
				{
					snprintf(ipv6buf, BUFFERSIZE, ":%x%x",
							addr->in6_u.u6_addr8[i*2] ,addr->in6_u.u6_addr8[i*2+1]);
				}
			}

		    strncat(buf, ipv6buf, BUFFERSIZE);
		}
	}
}

void net_refresh()
{
	do_swapint(&cur_net_count, &work_net_count);
	do_swapptr(&cur_net_list, &work_net_list);
}

void net_dump()
{
    char buf[BUFFERSIZE*2];

    struct in_addr laddr, raddr;
    struct in6_addr laddr6, raddr6;
    struct in6_addr swapladdr6, swapraddr6;
    unsigned lport, rport, state, uid;//, txq, rxq, num;
    int n;
    FILE *fp;

    sprintf(buf, "/proc/%d/net/tcp", getpid());
  	fp = fopen(buf, "r");

    if(fp != 0)
    {
        fgets(buf, BUFFERSIZE*2, fp);
        while(fgets(buf, BUFFERSIZE*2, fp))
        {
            n = sscanf(buf, " %*d: %x:%x %x:%x %x %*x:%*x %*x:%*x %*x %d",
                       &laddr.s_addr, &lport, &raddr.s_addr, &rport,
                       &state, &uid);

            if(n == 6)
				net_list_add("TCP", &laddr, lport, &raddr, rport, state, uid, 0);
        }
        fclose(fp);
    }

    snprintf(buf, BUFFERSIZE, "/proc/%d/net/tcp6", getpid());
  	fp = fopen(buf, "r");
    if(fp != 0)
    {
        fgets(buf, BUFFERSIZE*2, fp);
        while(fgets(buf, BUFFERSIZE*2, fp))
        {
            n = sscanf(buf, " %*d: %08X%08X%08X%08X:%x %08X%08X%08X%08X:%x %x %*x:%*x %*x:%*x %*x %d",
                       &laddr6.in6_u.u6_addr32[0], &laddr6.in6_u.u6_addr32[1],
                       &laddr6.in6_u.u6_addr32[2], &laddr6.in6_u.u6_addr32[3],
                       &lport,
                       &raddr6.in6_u.u6_addr32[0], &raddr6.in6_u.u6_addr32[1],
                       &raddr6.in6_u.u6_addr32[2], &raddr6.in6_u.u6_addr32[3],
                       &rport, &state, &uid);

   			swapladdr6.in6_u.u6_addr8[0] = laddr6.in6_u.u6_addr8[3];
   			swapladdr6.in6_u.u6_addr8[1] = laddr6.in6_u.u6_addr8[2];
   			swapladdr6.in6_u.u6_addr8[2] = laddr6.in6_u.u6_addr8[1];
   			swapladdr6.in6_u.u6_addr8[3] = laddr6.in6_u.u6_addr8[0];

            swapladdr6.in6_u.u6_addr8[4] = laddr6.in6_u.u6_addr8[7];
   			swapladdr6.in6_u.u6_addr8[5] = laddr6.in6_u.u6_addr8[6];
   			swapladdr6.in6_u.u6_addr8[6] = laddr6.in6_u.u6_addr8[5];
   			swapladdr6.in6_u.u6_addr8[7] = laddr6.in6_u.u6_addr8[4];

   			swapladdr6.in6_u.u6_addr8[8] = laddr6.in6_u.u6_addr8[11];
   			swapladdr6.in6_u.u6_addr8[9] = laddr6.in6_u.u6_addr8[10];
   			swapladdr6.in6_u.u6_addr8[10] = laddr6.in6_u.u6_addr8[9];
   			swapladdr6.in6_u.u6_addr8[11] = laddr6.in6_u.u6_addr8[8];

   			swapladdr6.in6_u.u6_addr8[12] = laddr6.in6_u.u6_addr8[15];
   			swapladdr6.in6_u.u6_addr8[13] = laddr6.in6_u.u6_addr8[14];
   			swapladdr6.in6_u.u6_addr8[14] = laddr6.in6_u.u6_addr8[13];
   			swapladdr6.in6_u.u6_addr8[15] = laddr6.in6_u.u6_addr8[12];


   			swapraddr6.in6_u.u6_addr8[0] = raddr6.in6_u.u6_addr8[3];
   			swapraddr6.in6_u.u6_addr8[1] = raddr6.in6_u.u6_addr8[2];
   			swapraddr6.in6_u.u6_addr8[2] = raddr6.in6_u.u6_addr8[1];
   			swapraddr6.in6_u.u6_addr8[3] = raddr6.in6_u.u6_addr8[0];

   			swapraddr6.in6_u.u6_addr8[4] = raddr6.in6_u.u6_addr8[7];
   			swapraddr6.in6_u.u6_addr8[5] = raddr6.in6_u.u6_addr8[6];
   			swapraddr6.in6_u.u6_addr8[6] = raddr6.in6_u.u6_addr8[5];
   			swapraddr6.in6_u.u6_addr8[7] = raddr6.in6_u.u6_addr8[4];

   			swapraddr6.in6_u.u6_addr8[8] = raddr6.in6_u.u6_addr8[11];
   			swapraddr6.in6_u.u6_addr8[9] = raddr6.in6_u.u6_addr8[10];
   			swapraddr6.in6_u.u6_addr8[10] = raddr6.in6_u.u6_addr8[9];
   			swapraddr6.in6_u.u6_addr8[11] = raddr6.in6_u.u6_addr8[8];

   			swapraddr6.in6_u.u6_addr8[12] = raddr6.in6_u.u6_addr8[15];
   			swapraddr6.in6_u.u6_addr8[13] = raddr6.in6_u.u6_addr8[14];
   			swapraddr6.in6_u.u6_addr8[14] = raddr6.in6_u.u6_addr8[13];
   			swapraddr6.in6_u.u6_addr8[15] = raddr6.in6_u.u6_addr8[12];


            if(n == 12)
				net_list_add("TCP6", &swapladdr6, lport, &swapraddr6, rport, state, uid, 1);
        }
        fclose(fp);
    }

    snprintf(buf, BUFFERSIZE*2, "/proc/%d/net/udp", getpid());
    fp = fopen(buf, "r");

    if(fp != 0) {
        fgets(buf, BUFFERSIZE*2, fp);
        while(fgets(buf, BUFFERSIZE*2, fp))
        {
            n = sscanf(buf, " %*d: %x:%x %x:%x %x %*x:%*x %*x:%*x %*x %d",
                       &laddr.s_addr, &lport, &raddr.s_addr, &rport,
                       &state, &uid);
            if(n == 6)
  				net_list_add("UDP", &laddr, lport, &raddr, rport, state, uid, 0);
        }
        fclose(fp);
    }

    snprintf(buf, BUFFERSIZE*2, "/proc/%d/net/udp6", getpid());
  	fp = fopen(buf, "r");
    if(fp != 0)
    {
        fgets(buf, BUFFERSIZE*2, fp);
        while(fgets(buf, BUFFERSIZE*2, fp))
        {
            n = sscanf(buf, " %*d: %08X%08X%08X%08X:%x %08X%08X%08X%08X:%x %x %*x:%*x %*x:%*x %*x %d",
                       &laddr6.in6_u.u6_addr32[0], &laddr6.in6_u.u6_addr32[1],
                       &laddr6.in6_u.u6_addr32[2], &laddr6.in6_u.u6_addr32[3],
                       &lport,
                       &raddr6.in6_u.u6_addr32[0], &raddr6.in6_u.u6_addr32[1],
                       &raddr6.in6_u.u6_addr32[2], &raddr6.in6_u.u6_addr32[3],
                       &rport, &state, &uid);

   			swapladdr6.in6_u.u6_addr8[0] = laddr6.in6_u.u6_addr8[3];
   			swapladdr6.in6_u.u6_addr8[1] = laddr6.in6_u.u6_addr8[2];
   			swapladdr6.in6_u.u6_addr8[2] = laddr6.in6_u.u6_addr8[1];
   			swapladdr6.in6_u.u6_addr8[3] = laddr6.in6_u.u6_addr8[0];

            swapladdr6.in6_u.u6_addr8[4] = laddr6.in6_u.u6_addr8[7];
   			swapladdr6.in6_u.u6_addr8[5] = laddr6.in6_u.u6_addr8[6];
   			swapladdr6.in6_u.u6_addr8[6] = laddr6.in6_u.u6_addr8[5];
   			swapladdr6.in6_u.u6_addr8[7] = laddr6.in6_u.u6_addr8[4];

   			swapladdr6.in6_u.u6_addr8[8] = laddr6.in6_u.u6_addr8[11];
   			swapladdr6.in6_u.u6_addr8[9] = laddr6.in6_u.u6_addr8[10];
   			swapladdr6.in6_u.u6_addr8[10] = laddr6.in6_u.u6_addr8[9];
   			swapladdr6.in6_u.u6_addr8[11] = laddr6.in6_u.u6_addr8[8];

   			swapladdr6.in6_u.u6_addr8[12] = laddr6.in6_u.u6_addr8[15];
   			swapladdr6.in6_u.u6_addr8[13] = laddr6.in6_u.u6_addr8[14];
   			swapladdr6.in6_u.u6_addr8[14] = laddr6.in6_u.u6_addr8[13];
   			swapladdr6.in6_u.u6_addr8[15] = laddr6.in6_u.u6_addr8[12];


   			swapraddr6.in6_u.u6_addr8[0] = raddr6.in6_u.u6_addr8[3];
   			swapraddr6.in6_u.u6_addr8[1] = raddr6.in6_u.u6_addr8[2];
   			swapraddr6.in6_u.u6_addr8[2] = raddr6.in6_u.u6_addr8[1];
   			swapraddr6.in6_u.u6_addr8[3] = raddr6.in6_u.u6_addr8[0];

   			swapraddr6.in6_u.u6_addr8[4] = raddr6.in6_u.u6_addr8[7];
   			swapraddr6.in6_u.u6_addr8[5] = raddr6.in6_u.u6_addr8[6];
   			swapraddr6.in6_u.u6_addr8[6] = raddr6.in6_u.u6_addr8[5];
   			swapraddr6.in6_u.u6_addr8[7] = raddr6.in6_u.u6_addr8[4];

   			swapraddr6.in6_u.u6_addr8[8] = raddr6.in6_u.u6_addr8[11];
   			swapraddr6.in6_u.u6_addr8[9] = raddr6.in6_u.u6_addr8[10];
   			swapraddr6.in6_u.u6_addr8[10] = raddr6.in6_u.u6_addr8[9];
   			swapraddr6.in6_u.u6_addr8[11] = raddr6.in6_u.u6_addr8[8];

   			swapraddr6.in6_u.u6_addr8[12] = raddr6.in6_u.u6_addr8[15];
   			swapraddr6.in6_u.u6_addr8[13] = raddr6.in6_u.u6_addr8[14];
   			swapraddr6.in6_u.u6_addr8[14] = raddr6.in6_u.u6_addr8[13];
   			swapraddr6.in6_u.u6_addr8[15] = raddr6.in6_u.u6_addr8[12];

   			if(n == 12)
				net_list_add("UDP6", &swapladdr6, lport, &swapraddr6, rport, state, uid, 1);
        }
        fclose(fp);
    }

    return;
}

/* Interface */
if_info* cur_ifinfo = (void *) 0;
if_info* work_ifinfo = (void *) 0;
int work_if_count = 0;
void *work_if_list = (void *) 0;
int cur_if_count = 0;
void *cur_if_list = (void *) 0;

void if_list_add(if_info *new_if)
{
	// create new node
	if_info* new_node = (if_info *) malloc(sizeof(if_info));

	memcpy(new_node, new_if, sizeof(if_info));
	new_node->next = (void *) 0;

	// find node
	if(work_if_list != (void *) 0)
	{
		if_info *end = work_if_list;
		while(end->next != (void *) 0)
			end = end->next;

		end->next = new_node;
	}
	else
	{
		work_if_list = new_node;
	}
}

void if_list_empty()
{
	if(work_if_list == (void *) 0)
		return;

	// reset
	if_info *old_node = (if_info *) work_if_list;
	work_if_list = (void *) 0;
	work_if_count = 0;

	// release memory
	if_info *next_node = (void *) 0;
	while(old_node->next != (void *) 0)
	{
		next_node = (if_info *) old_node->next;
		free(old_node);

		old_node = next_node;
	}
	free(old_node);

	return;
}

void if_skipline(FILE *f)
{
  int ch;
  do {
    ch = getc(f);
  } while ( ch != '\n' && ch != EOF );
}

int if_list_work_next()
{
	if(work_ifinfo == (void *) 0)
		work_ifinfo = work_if_list;
	else
	{
		work_ifinfo = work_ifinfo->next;
	}

	if(work_ifinfo == (void *) 0)
		return 0;
	return 1;
}

void if_list_work_reset()
{
	work_ifinfo = (void *) 0;
}

void if_refresh()
{
	do_swapint(&cur_if_count, &work_if_count);
	do_swapptr(&cur_if_list, &work_if_list);
}

void if_dump()
{
	char buf[BUFFERSIZE];
	FILE *pnd;
	int mac;
	if_info ifc;

	struct ifreq ifr;
    int s;


	int n = 0;

	//	pnd = fopen("/proc/net/dev", "r");
	snprintf(buf, BUFFERSIZE, "/proc/%d/net/dev", getpid());
	pnd = fopen(buf, "r");
    if(pnd !=0)
    {
		if_skipline(pnd);
    	if_skipline(pnd);

    	do
    	{
    		memset(&ifc, 0, sizeof(ifc));

    		n = fscanf(pnd, " %[^:]:%u %u %u %u %u %u %u %u %u %u %u %u %u %u %u",
    	              &ifc.name,
    	              &ifc.r_bytes, &ifc.r_pkt, &ifc.r_err, &ifc.r_drop,
    	              &ifc.r_fifo, &ifc.r_frame, &ifc.r_compr, &ifc.r_mcast,
    	              &ifc.x_bytes, &ifc.x_pkt, &ifc.x_err, &ifc.x_drop,
    	              &ifc.x_fifo, &ifc.x_coll, &ifc.x_carrier, &ifc.x_compr);

    		if(n == 16)
    		{
    		    memset(&ifr, 0, sizeof(struct ifreq));
    		    strncpy(ifr.ifr_name, ifc.name, sizeof(ifc.name));

    		    if((s = socket(AF_INET, SOCK_DGRAM, 0)) >= 0) {
        			if (ioctl(s, SIOCGIFADDR, &ifr) >= 0)
       		            ifc.addr.s_addr = ((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr.s_addr;

       		        if (ioctl(s, SIOCGIFNETMASK, &ifr) >= 0)
       		            ifc.mask = ((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr.s_addr;

       		        if (ioctl(s, SIOCGIFFLAGS, &ifr) >= 0)
       		            ifc.flags = ifr.ifr_flags;

					snprintf(buf, BUFFERSIZE, "/sys/class/net/%s/address", ifc.name);
       		        if((mac = open(buf, O_RDONLY)) > 0)
       		        {
       		        	read(mac, ifc.mac, 17);
       		        	close(mac);
       		        }

       		        if(strlen(ifc.mac) < 17)
       		        	ifc.mac[0] = 0;

       		        close(s);
    		    }


    		    if_list_add(&ifc);
    		    work_if_count++;
    		}

    		if_skipline(pnd);

    	} while(!feof(pnd));
    	fclose(pnd);
    }

	snprintf(buf, BUFFERSIZE, "/proc/%d/net/if_inet6", getpid());
	pnd = fopen(buf, "r");
    if(pnd !=0)
    {
    	int tmpmark6;
    	struct in6_addr tmpaddr, swapaddr;
    	char tmpname[16];

    	tmpname[0] = 0;

    	//00000000000000000000000000000001 01 80 10 80       lo
    	do
    	{
    		memset(&tmpaddr, 0, sizeof(tmpaddr));

    		n = fscanf(pnd, "%8X%8X%8X%8X %*d %x %*d %*d %s",
    	              &tmpaddr.in6_u.u6_addr32[0], &tmpaddr.in6_u.u6_addr32[1],
    	              &tmpaddr.in6_u.u6_addr32[2], &tmpaddr.in6_u.u6_addr32[3],
    	              &tmpmark6, &tmpname
					  );

   			swapaddr.in6_u.u6_addr8[0] = tmpaddr.in6_u.u6_addr8[3];
   			swapaddr.in6_u.u6_addr8[1] = tmpaddr.in6_u.u6_addr8[2];
   			swapaddr.in6_u.u6_addr8[2] = tmpaddr.in6_u.u6_addr8[1];
   			swapaddr.in6_u.u6_addr8[3] = tmpaddr.in6_u.u6_addr8[0];

   			swapaddr.in6_u.u6_addr8[4] = tmpaddr.in6_u.u6_addr8[7];
   			swapaddr.in6_u.u6_addr8[5] = tmpaddr.in6_u.u6_addr8[6];
   			swapaddr.in6_u.u6_addr8[6] = tmpaddr.in6_u.u6_addr8[5];
   			swapaddr.in6_u.u6_addr8[7] = tmpaddr.in6_u.u6_addr8[4];

   			swapaddr.in6_u.u6_addr8[8] = tmpaddr.in6_u.u6_addr8[11];
   			swapaddr.in6_u.u6_addr8[9] = tmpaddr.in6_u.u6_addr8[10];
   			swapaddr.in6_u.u6_addr8[10] = tmpaddr.in6_u.u6_addr8[9];
   			swapaddr.in6_u.u6_addr8[11] = tmpaddr.in6_u.u6_addr8[8];

   			swapaddr.in6_u.u6_addr8[12] = tmpaddr.in6_u.u6_addr8[15];
   			swapaddr.in6_u.u6_addr8[13] = tmpaddr.in6_u.u6_addr8[14];
   			swapaddr.in6_u.u6_addr8[14] = tmpaddr.in6_u.u6_addr8[13];
   			swapaddr.in6_u.u6_addr8[15] = tmpaddr.in6_u.u6_addr8[12];

    		if(n == 6)
    		{
    			if_list_work_reset();
				while(if_list_work_next())
				{
					if(strcmp(work_ifinfo->name, tmpname) == 0)
					{
						work_ifinfo->addr6 = swapaddr;
						work_ifinfo->mask6 = tmpmark6;
						break;
					}
				}
     		}

    		if_skipline(pnd);

    	} while(!feof(pnd));
    	fclose(pnd);
    }

}

void network_init()
{
	net_list_empty();
	if_list_empty();
	net_dump();
	if_dump();
	return;
}

int net_list_nextrecord()
{
	if(cur_netinfo == (void *) 0)
		cur_netinfo = cur_net_list;
	else
	{
		cur_netinfo = cur_netinfo->next;
	}

	if(cur_netinfo == (void *) 0)
		return 0;
	return 1;
}

int net_list_count()
{
	return cur_net_count;
}

void net_list_reset()
{
	cur_netinfo = (void *) 0;
}

int net_list_setpositon(int position)
{
	if(position == -1)
		return 1;

	net_list_reset();
	while(position >= 0)
	{
		if(!net_list_nextrecord())
			return 0;
		position--;
	}
	return 1;
}

void net_get_protocol(int position, char *buf)
{
	if(!net_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	strncpy(buf, cur_netinfo->protocol, BUFFERSIZE);
	return;
}

void net_get_localip(int position, char *buf)
{
	if(!net_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	if(cur_netinfo->tcp6flag == 1)
		net_addr2str_ipv6(&cur_netinfo->localip6, buf);
	else
		net_addr2str_ipv4(&cur_netinfo->localip, buf);
	return;
}

int net_get_localport(int position)
{
	if(!net_list_setpositon(position))
		return 0;

	return cur_netinfo->localport;
}


void net_get_remoteip(int position, char *buf)
{
	if(!net_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	if(cur_netinfo->tcp6flag == 1)
		net_addr2str_ipv6(&cur_netinfo->remoteip6, buf);
	else
		net_addr2str_ipv4(&cur_netinfo->remoteip, buf);
	return;
}

int net_get_remoteport(int position)
{
	if(!net_list_setpositon(position))
		return 0;

	return cur_netinfo->remoteport;
}


void net_get_status(int position, char *buf)
{
	if(!net_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	if(strcmp(cur_netinfo->protocol, "UDP") == 0)
		strncpy(buf, "", BUFFERSIZE);
	else
		strncpy(buf, net_state2str(cur_netinfo->status), BUFFERSIZE);
	return;
}

int net_get_uid(int position)
{
	if(!net_list_setpositon(position))
		return 0;
	return  cur_netinfo->uid;
}

int if_list_count()
{
	return cur_if_count;
}

int if_list_nextrecord()
{
	if(cur_ifinfo == (void *) 0)
		cur_ifinfo = cur_if_list;
	else
	{
		cur_ifinfo = cur_ifinfo->next;
	}

	if(cur_ifinfo == (void *) 0)
		return 0;
	return 1;
}

void if_list_reset()
{
	cur_ifinfo = (void *) 0;
}

int if_list_setpositon(int position)
{
	if(position == -1)
		return 1;

	if_list_reset();
	while(position >= 0)
	{
		if(!if_list_nextrecord())
			return 0;
		position--;
	}
	return 1;
}

void if_get_name(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	strncpy(buf, cur_ifinfo->name, BUFFERSIZE);
	return;
}

void if_get_addr(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	net_addr2str_ipv4(&cur_ifinfo->addr, buf);

    return;
}

void if_get_addr6(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	net_addr2str_ipv6(&cur_ifinfo->addr6, buf);

    return;
}


void if_get_mac(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

    snprintf(buf, BUFFERSIZE, "%s", cur_ifinfo->mac);
    return;
}

void if_get_netmask(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

    snprintf(buf, BUFFERSIZE, "%d.%d.%d.%d",
    		cur_ifinfo->mask & 0xff,
            ((cur_ifinfo->mask >> 8) & 0xff),
            ((cur_ifinfo->mask >> 16) & 0xff),
            ((cur_ifinfo->mask >> 24) & 0xff));
    return;
}

void if_get_netmask6(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

    snprintf(buf, BUFFERSIZE, "%d", cur_ifinfo->mask6);

    return;
}


void if_get_scope(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	if(IN6_IS_ADDR_LOOPBACK(&cur_ifinfo->addr6))
		strcpy(buf, "Host");
	else if(IN6_IS_ADDR_LINKLOCAL(&cur_ifinfo->addr6))
		strcpy(buf, "Link");
	else if(IN6_IS_ADDR_SITELOCAL(&cur_ifinfo->addr6))
		strcpy(buf, "Site");
	else
		strcpy(buf, "Global");

	return;
}

void if_get_flags(int position, char *buf)
{
	if(!if_list_setpositon(position))
	{
		buf[0] = 0;
		return;
	}

	char *updown, *brdcst, *loopbk, *ppp, *running, *multi;

	updown =  (cur_ifinfo->flags & IFF_UP)           ? "up" : "down";
    brdcst =  (cur_ifinfo->flags & IFF_BROADCAST)    ? " broadcast" : "";
    loopbk =  (cur_ifinfo->flags & IFF_LOOPBACK)     ? " loopback" : "";
	ppp =     (cur_ifinfo->flags & IFF_POINTOPOINT)  ? " point-to-point" : "";
	running = (cur_ifinfo->flags & IFF_RUNNING)      ? " running" : "";
	multi =   (cur_ifinfo->flags & IFF_MULTICAST)    ? " multicast" : "";

	snprintf(buf, BUFFERSIZE, "%s%s%s%s%s%s", updown, brdcst, loopbk, ppp, running, multi);

	return;
}

unsigned int if_get_outsize(int position)
{
	if(!if_list_setpositon(position))
		return 0;

	return cur_ifinfo->x_bytes;
}

unsigned int if_get_insize(int position)
{
	if(!if_list_setpositon(position))
		return 0;

	return cur_ifinfo->r_bytes;
}
