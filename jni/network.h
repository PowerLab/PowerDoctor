#include "osmonitor.h"
#include "func.h"

void network_init();

/* Network Connections Module */
typedef struct netinfo_proto
{
	char protocol[4];

	union
	{
		struct in_addr localip;
		struct in6_addr localip6;
	};

	unsigned localport;

	union
	{
		struct in_addr remoteip;
		struct in6_addr remoteip6;
	};

	unsigned remoteport;

	unsigned status;

	unsigned tcp6flag;

	int uid;

	void *next;
} net_info;


void net_refresh();
void net_dump();

void net_list_add(char *new_protocol, void *new_localip, unsigned new_localport,
				 void *new_remoteip, unsigned new_remoteport, unsigned new_status,
				 unsigned new_uid, int flag);
int net_list_nextrecord();
void net_list_reset();
void net_list_empty();
int net_list_count();
int net_list_setpositon(int position);

char *net_state2str(unsigned state);
void net_addr2str_ipv4(struct in_addr *addr, char *buf);
void net_addr2str_ipv6(struct in6_addr *addr, char *buf);
void net_set_ip6to4(int value);

void net_get_protocol(int position, char *buf);
void net_get_localip(int position, char *buf);
int net_get_localport(int position);
void net_get_remoteip(int position, char *buf);
int net_get_remoteport(int position);
void net_get_status(int position, char *buf);
int net_get_uid(int position);

typedef struct ifinfo_proto {
  char name[16];
  char mac[24];
  struct in_addr addr;
  struct in6_addr addr6;
  unsigned int flags, mask, mask6;
  unsigned int r_bytes, r_pkt, r_err, r_drop, r_fifo, r_frame;
  unsigned int r_compr, r_mcast;
  unsigned int x_bytes, x_pkt, x_err, x_drop, x_fifo, x_coll;
  unsigned int x_carrier, x_compr;
  void *next;
} if_info;


void if_refresh();
void if_dump();
void if_skipline(FILE *f);

void if_list_add(if_info *new_if);
void if_list_empty();
int if_list_count();
int if_list_nextrecord();
void if_list_reset();
int if_list_setpositon(int position);

int if_list_work_next();
void if_list_work_reset();

void if_get_name(int position, char *buf);
void if_get_addr(int position, char *buf);
void if_get_addr6(int position, char *buf);
void if_get_mac(int position, char *buf);
void if_get_netmask(int position, char *buf);
void if_get_netmask6(int position, char *buf);
void if_get_scope(int position, char *buf);
void if_get_flags(int position, char *buf);
unsigned int if_get_outsize(int position);
unsigned int if_get_insize(int position);
