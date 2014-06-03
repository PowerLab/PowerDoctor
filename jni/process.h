#include "osmonitor.h"
#include "func.h"


/* CPU Module */
typedef struct cpuinfo_proto
{
	unsigned long user, nice, system, idle, iowait, irq, softirq;
} cpu_info;

void cpu_init();
void cpu_dump();
void cpu_refresh();
void cpu_refresh_usage();

void cpu_get_usage(char *buf);
int cpu_get_usagevalue();

/* Memory Module */
typedef struct meminfo_proto
{
	unsigned long memtotal;
	unsigned long memfree;
	unsigned long buffers;
	unsigned long cached;
} mem_info;

void mem_dump();
unsigned long mem_get_total();
unsigned long mem_get_free();
unsigned long mem_get_cached();
unsigned long mem_get_buffers();

/* Process Module*/
typedef struct processinfo_proto
{
	char name[80];
	char owner[80];
	char status;
	unsigned int uid, pid, load, threadnum;
	unsigned long rss;
	unsigned long delta_utime;
	unsigned long delta_stime;
	double delta_load;
	void *next;
} process_info;

enum sort_type_proto
{
	byPID = 1, byLoad, byMem, byThreads, byName
} ps_sort_type;

void ps_list_add(process_info *new_ps);
void ps_list_empty(process_info **work_ps_list);
int ps_list_nextrecord(process_info **work_list, process_info **work_ptr);
void ps_list_reset(process_info **work_ptr);
int ps_list_setposition(process_info **work_list, process_info **work_ptr, int position);
long ps_list_count();

void ps_list_work_empty();

void ps_refresh();
void ps_dump();
void ps_init();
void ps_uninit();

void ps_instance_dump(int pid);
void ps_instance_swap(process_info *x,process_info *y);
void ps_sort();
void ps_bubblesort(process_info **work_list, int n);
void ps_refresh_load();

void ps_set_filter(int value);
void ps_set_algorithm(int value);
void ps_set_sort(int value);
void ps_set_order(int value);

int ps_get_pid(int position);
int ps_get_uid(int position);
int ps_get_load(int position);
unsigned long ps_get_utime(int position);
unsigned long ps_get_stime(int position);
void ps_get_status(int position, char *buf);
void ps_get_name(int position, char *buf);
void ps_get_owner(int position, char *buf);
int ps_get_rss(int position);
int ps_get_threadnum(int position);
void ps_get_name_by_uid(int uid, char *buf);
