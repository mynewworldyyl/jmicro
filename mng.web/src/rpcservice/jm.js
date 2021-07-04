const ROOT = '/jmicro/JMICRO';
export default  {

  ROOT,
  MNG : 'mng',
  LOG2LEVEL : {2:'LOG_DEBUG', 5:'LOG_ERROR', 6:'LOG_FINAL', 3:'LOG_INFO', 0:'LOG_NO',
  1: 'LOG_TRANCE', 4:'LOG_WARN'},

  RES_STATUS: {"1":"Uploading","2":"Ready","3":"Enable","4":"Error","5":"WAITING","6":"Download"},
  DEP_STATUS:{'1':'Draft','2':'Enable','3':"Check"},
  ROUTER_ROOT : ROOT + '/routeRules',
    CONFIG_ROOT : ROOT,
  AGENT_ROOT : ROOT + '/choreography/agents',
  INSTANCE_ROOT : ROOT + '/choreography/instances',
  DEP_ROOT : ROOT + '/choreography/deployments',

  RULE_ID: 'cn.jmicro.api.route.RouteRule',

}
