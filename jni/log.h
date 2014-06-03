#include "osmonitor.h"

struct logger_entry {
    uint16_t    len;    /* length of the payload */
    uint16_t    __pad;  /* no matter what, we get 2 bytes of padding */
    int32_t     pid;    /* generating process's pid */
    int32_t     tid;    /* generating process's tid */
    int32_t     sec;    /* seconds since Epoch */
    int32_t     nsec;   /* nanoseconds */
    char        msg[0]; /* the entry's payload */
};

#define LOGGER_LOG_MAIN		"log/main"
#define LOGGER_LOG_RADIO	"log/radio"
#define LOGGER_LOG_EVENTS	"log/events"

#define LOGGER_ENTRY_MAX_LEN		(4*1024)
#define LOGGER_ENTRY_MAX_PAYLOAD	(LOGGER_ENTRY_MAX_LEN - sizeof(struct logger_entry))

#define __LOGGERIO	0xAE
#define LOGGER_GET_LOG_BUF_SIZE     _IO(__LOGGERIO, 1) /* size of log */
#define LOGGER_GET_LOG_LEN          _IO(__LOGGERIO, 2) /* used log len */
#define LOGGER_GET_NEXT_ENTRY_LEN   _IO(__LOGGERIO, 3) /* next entry len */

/* klog */
#define KLOG_BUF_SHIFT	17	/* CONFIG_LOG_BUF_SHIFT from our kernel */
#define KLOG_BUF_LEN	(1 << KLOG_BUF_SHIFT)

/* logcat */
#define DEFAULT_LOG_ROTATE_SIZE_KBYTES 16
#define DEFAULT_MAX_ROTATED_LOGS 4
#define RECORD_LENGTH_FIELD_SIZE_BYTES sizeof(uint32_t)
#define LOG_FILE_DIR    "/dev/log/"

void log_init();

/* DMesg */
typedef struct dmesg_proto {
  char level;
  long sec;
  char msg[BUFFERSIZE*2];
  void *next;
} dmesg_item;

// manipulate list
int dmesg_list_count();
void dmesg_list_add(dmesg_item *new_dmesg);
void dmesg_list_empty();
int dmesg_list_nextrecord();
void dmesg_list_reset();
int dmesg_list_setpositon(int position);

// get list info
void dmesg_get_level(int position, char *buf);
void dmesg_get_time(int position, char *buf);
void dmesg_get_msg(int position, char* buf);

// set filter
void dmesg_set_filter(int value);
void dmesg_set_filter_level(char value);
void dmesg_set_filter_string(const char *value);

// refresh
void dmesg_dump();
void dmesg_refresh();
int dmesg_check();
void dmesg_init();

/* LogCat */
typedef struct logcat_proto {
    time_t tv_sec;
    long tv_nsec;
    android_LogPriority priority;
    int pid;
    char tag[64];
	void *next;
    char message[1];
} logcat_info;

void logcat_list_add(logcat_info *new_logcat, char *msg);
void logcat_list_empty();
int logcat_list_nextrecord();
int logcat_list_count();
void logcat_list_reset();
int logcat_list_setpositon(int position);

int logcat_get_logsize();
int logcat_get_logreadablesize();
void logcat_get_level(int position, char *buf);
void logcat_get_time(int position, char *buf);
int logcat_get_pid(int position);
void logcat_get_tag(int position, char* buf);
char *logcat_get_msg(int position);
void logcat_get_readloglines(int logfd);

void logcat_set_source(int value);
void logcat_set_filter(int value);
void logcat_set_pid(int value);
void logcat_set_filter_level(int value);
void logcat_set_filter_string(char *value);

void logcat_dump();
void logcat_refresh();
int logcat_check();
void logcat_init();
