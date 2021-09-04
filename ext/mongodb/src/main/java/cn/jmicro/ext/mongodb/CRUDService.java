package cn.jmicro.ext.mongodb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.PersistVo;
import cn.jmicro.api.QryDefJRso;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CRUDService<T extends PersistVo> {
	
	public static final String ID = IObjectStorage._ID;

	private IObjectStorage os;
	
	private ComponentIdServer idGenerator;
	
	private String table;
	
	public CRUDService(IObjectStorage os,ComponentIdServer idGenerator,String table) {
		this.os = os;
		this.idGenerator = idGenerator;
		this.table = table;
	}

	public boolean add(Class<T> clazz,T vo) {
		vo.setCreatedTime(TimeUtils.getCurTime());
		vo.setUpdatedTime(TimeUtils.getCurTime());
		vo.setId(idGenerator.getLongId(clazz));
		return os.save(table, vo, clazz, false, false);
	}

	public boolean deleteById(Class<T> clazz,Long id) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(ID, id);
		return os.deleteById(table, id, ID);
	}
	
	public boolean deleteFlagById(Class<T> clazz,Long id) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(ID, id);
		Map<String,Object> updater = new HashMap<>();
		updater.put("deleted", true);
		return os.update(table, filter, updater, clazz) > 0;
	}
	
	public boolean update(Class<T> clazz,Map<String,Object> filter,Map<String,Object> updater) {
		return os.update(table, filter, updater, clazz) > 0;
	}
	
	public boolean updateById(Class<T> clazz, T val) {
		val.setUpdatedTime(System.currentTimeMillis());
		return os.updateById(table, val, clazz, ID, false);
	}
	
	public T getById(Class<T> clazz,Long id) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(ID, id);
		return os.getOne(table, filter, clazz);
	}
	
	public boolean exist(Map<String,Object> filter) {
		return os.count(table, filter) > 0;
	}
	
	public <T> T getOne(Class<T> clazz,Map<String,Object> filter) {
		return os.getOne(table, filter, clazz);
	}

	public RespJRso<List<T>>  query(Class<T> clazz, QueryJRso qry) {
		
		RespJRso<List<T>> r = new RespJRso<>();
		
		Map<String,Object> filter = new HashMap<>();
		//filter.put("deleted", false);
		
		List<QryDefJRso> ps = qry.getPs();
		
		for(QryDefJRso qd : ps) {
			if(qd.getV() == null) continue;//排除空值
			
			switch(qd.getOpType()) {
			case QryDefJRso.OP_EQ:
				filter.put(qd.getFn(), qd.getV());
				break;
			case QryDefJRso.OP_GT:
				Map<String,Object> gt = new HashMap<>();
				gt.put("$gt", qd.getV());
				filter.put(qd.getFn(), gt);
				break;
			case QryDefJRso.OP_GTE:
				Map<String,Object> gte = new HashMap<>();
				gte.put("$gte", qd.getV());
				filter.put(qd.getFn(), gte);
				break;
			case QryDefJRso.OP_IN:
				String[] arr = qd.getV().toString().split(",");
				Map<String,Object> in = new HashMap<>();
				in.put("$in", Arrays.asList(arr));
				filter.put(qd.getFn(), in);
				break;
			case QryDefJRso.OP_LT:
				filter.put(qd.getFn(), qd.getV());
				break;
			case QryDefJRso.OP_LTE:
				filter.put(qd.getFn(), qd.getV());
				break;
			case QryDefJRso.OP_REGEX:
				Map<String,Object> re = new HashMap<>();
				re.put("$regex", qd.getV());
				filter.put(qd.getFn(), re);
				break;
			default:
				log.error("Not support OP type [{}],field [{}], clazz [{}] ",qd.getOpType(),qd.getFn(),clazz.getName() );
			}
		}
		
		int cnt =(int) os.count(table, filter);
		r.setTotal(cnt);
		
		if(cnt > 0) {
			List<T> list = this.os.query(table, filter, clazz,
					qry.getPageSize(), qry.getCurPage()-1,null,qry.getSortName(),qry.getOrder());
			r.setData(list);
		}
		
		return r;

	}
}
