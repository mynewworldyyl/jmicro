//非务务中执行SQL
let executeSqlWithoutTx = null

//在事务中执行SQL
let executeSql = null
let beginTx = null
let commitTx = null
//let rollbackTx = null

let dbName = null
let conn = null

let init = null

let enDefs = {}//表名到实体定义

function booleanToInt(value) {
	if (typeof value == 'boolean') {
		if (value) {
			value = 1;
		} else {
			value = 0;
		}
	}
	return value
}

function intToBoolean(value) {
	return value == 1;
}
	

if ((typeof plus != 'undefined' && plus.sqlite)){
	conn = plus.sqlite
	//h5 plus sql Context
	console.log('plus sql Context')
	
	init = (opts)=>{
		dbName = opts.dbName
		return new Promise((reso, reje) => {
			console.log("Open h5 plus database")
			console.log(plus.sqlite.openDatabase)
			
			let isopen = plus.sqlite.isOpenDatabase({
				name: dbName,
				path: '_doc/' + dbName + '.db',
			});
			
			if(isopen) {
				console.log(dbName + " is open status")
				reso({code:0})
				return
			}
			
			plus.sqlite.openDatabase({
				name: dbName,
				path: '_doc/' + dbName + '.db',
				success: function(e) {
					console.log('open plus.sqlite database successfully');
					reso({
						code: 0
					})
				},
				fail: function(e) {
					console.log('open plus.sqlite database  failed: ' + JSON.stringify(e));
					reje({
						code: 1,
						msg: JSON.stringify(e)
					})
				}
			});
		})
	}
	
	let exTx = (opType)=>{
		return new Promise((reso,reje)=>{
			console.log("tx: " + opType)
			plus.sqlite.transaction({
				name: dbName,
				operation: opType,
				success: (e) => {
					reso({code: 0, data: JSON.stringify(e)})//执行成功
				},
				fail: (e) => {
					console.log('failed: '+opType+' error： ' + JSON.stringify(e))
					reje({code: 1,msg: JSON.stringify(e)})
				}
			});
		})
	}
	
	let rollbackTx = ()=>{
		return exTx("rollback")
	}
	
	commitTx = ()=>{
		return exTx("commit")
	}
	
	beginTx = ()=>{
		return exTx("begin")
	}
	
	let doExeSql = (sql,inTx) => {
		sql = sql.trimLeft()
		if(sql.startsWith("select") || sql.startsWith("SELECT")) {
			return new Promise((reso,reje)=>{
				plus.sqlite.selectSql({
					name: dbName,
					sql: sql,
					success: (e) => {
						console.log("select SQL: " + sql)
						console.log('selectSql success: ' + JSON.stringify(e))
						reso({code:0,data:e,tx:inTx})//SQL执行成功
					},
					fail: (e) => {
						console.log("select SQL: " + sql)
						console.log('selectSql failed: ' + JSON.stringify(e))
						rollbackTx()//执行失败，内部直接回滚
						reje({code:1,msg:e,tx:inTx})
					}
				});
			})
		} else {
			return new Promise((reso,reje)=>{
				plus.sqlite.executeSql({
					name: dbName,
					sql: sql,
					success: (e) => {
						console.log("Non select SQL success: " + sql)
						console.log('Non select SQL success: ' + JSON.stringify(e))
						reso({code:0, data:e, tx:inTx})//SQL执行成功
					},
					fail: (e) => {
						console.log("Non select SQL fail: " + sql)
						console.log('Non select SQL failed: ' + JSON.stringify(e))
						//rollbackTx()//执行失败，内部直接回滚
						reje({code:1,msg:e,tx:inTx})
					}
				});
			})
		}
	}

	executeSql = (sql,tx)=>{
		let self = this
		return new Promise((reso, reje) => {
			sql = sql.trim()
			let isNeedTx = sql.startWith("UPDATE") || sql.startWith("update") 
				|| sql.startWith("DELETE") || sql.startWith("delete") 
				|| sql.startWith("INSERT") || sql.startWith("insert")
			if(!isNeedTx) {
				//非事务类操作
				doExeSql(sql,true)
				.then(rst=>{reso(rst)})
				.catch(rst=> {reje(rst)})
			} else {
				//更新，删除需要事务类操作
				beginTx()
				.then(rst=>{
					console.log(rst,sql)
					if(rst.code == 0) {
						doExeSql(sql,true)
						.then(rst0=>{
							commitTx()
							reso(rst0)
						})
						.catch(rst0=> {
							rollbackTx()
							reje(rst0)
							})
					}else {
						rollbackTx()
						reje(rst)
					}
				})
			}
		})
	}
		
	executeSqlWithoutTx = (sql) => {
		return doExeSql(sql)
	}
	
} else if(typeof window != 'undefined' && window.openDatabase) {

	init = (opts)=>{
		return new Promise((reso, reje) => {
			dbName = opts.dbName
			let ver = opts.version?opts.version:'1.0.0'
			conn = window.openDatabase(dbName, ver, 'not support database', 1*1024 * 1024);
			if (conn) {
				console.log('open window.openDatabase successfully');
			    reso({code:0,data:true})
			} else {
			    console.log('open window.openDatabase fail');
				reso({code:1,data:false})
			}
		})
	}
	
	let rollbackTx = (tx)=>{
		return new Promise((reso,reje)=>{
			try{
				if(tx) {
					//tx.ScrollOffsetCallbackResult
					reso({code:0,data:true})
				}else {
					reje({code:1, msg:"非事务上下文中不能回滚事务"})
				}
			}catch(e){
				reje({ code:2,msg:e })
			}
		})
	}
	
	commitTx = (tx)=>{
		return new Promise((reso,reje)=>{
			try{
				if(tx) {
					tx.commit()
					reso({code:0,data:true})
				}else {
					reje({code:1,msg:"非事务上下文中不能提交事务"})
				}
			}catch(e){
				reje({ code:2,msg:e })
			}
		})
	}
	
	beginTx = ()=>{
		return new Promise((reso,reje)=>{
			if(!conn && !init({})) {
				reje({code:1,msg:'打开本地数据库失败'})
			}else {
				conn.transaction((tx)=>{
					reso({code:0,tx:tx,data:tx})
				})	
			}
		})
	}
	
	let doExeSql = (sql,tx) => {
		
		let fun = (reso,reje)=>{
			tx.executeSql(sql, [],
				function (tx2, results) {
					console.log(sql)
					console.log("results: ",results)
					let rst = {code:0, data:[], tx:tx2}
					if(results.rows) {
						rst.data = results.rows
					}else {
						rst.data = []
					}
					
					try{
						if(results.insertId) {
							rst.insertId = results.insertId
						}
					}catch(e){}
					
					try{
						if(results.rowsAffected) {
							rst.count = results.rowsAffected
						}
					}catch(e){}
					
					reso(rst)
				},
				function (tx2, error) {
					console.log(sql)
					console.log(error)
					rollbackTx(tx2) //回滚事务
					reso({code:1, msg:error, tx:tx2})
				}
			)
		}
		
		return new Promise((reso,reje)=>{
			if(!conn) {
				init().then(rst=>{
					if(rst.code == 0) {
						fun(reso,reje)
					}else {
						console.log(rst)
						console.log("Fail: "+sql)
						reje(rst)
					}
				})
			}else {
				fun(reso,reje)
			}	
		})
	}
	
	executeSqlWithoutTx = executeSql = (sql,tx)=>{
		let self = this
		return new Promise((reso,reje)=>{
			if(tx) {
				doExeSql(sql,tx).then(rst0=>{reso(rst0)}).catch(rst0=>{reje(rst0)}) 
			} else {
				beginTx()
				.then(rst=>{
					if(rst.code == 0) {
						doExeSql(sql,rst.tx).then(rst0=>{reso(rst0)}).catch(rst0=>{reje(rst0)})
					}else {
						reje(rst)
					}
				})
			}
		})
	};
} 

class Builder{
	constructor(table) {
		this._checkTypeOf(table,'string','table name cannot be null')
		this.table = table
		this.wclause = ""// Where clause
	}
	
	andIn(colName,valArray) {
		return this._in(colName,valArray,' IN ')
	}
	
	andNotIn(colName,valArray) {
		return this._in(colName,valArray,' NOT IN ')
	}
	
	andNotLike(colName,val) {
		return this._like(colName,val,' NOT LIKE %"' + val + '%" ')
	}
	
	andLeftNotLike(colName,val) {
		return this._like(colName,val,'  NOT LIKE "' + val + '%" ')
	}
	
	andRightNotLike(colName,val) {
		return this._like(colName,val,'  NOT LIKE %"' + val + '" ')
	}
	
	andLike(colName,val) {
		return this._like(colName,val,' LIKE %"' + val + '%" ')
	}
	
	andLeftLike(colName,val) {
		return this._like(colName,val,' LIKE "' + val + '%" ')
	}
	
	andRightLike(colName,val) {
		return this._like(colName,val,' LIKE %"' + val + '" ')
	}
	
	andEquals(colName,val) {
		return this._and(colName,val,'=')
	}
	
	andNotEquals(colName,val) {
		return this._and(colName,val,' != ')
	}
	
	_in(colName,valArray,op) {
		this._checkColName(colName)
		if(!valArray && valArray.length == 0) {
			throw 'IN value cannot be NULL'
		}
		
		if(!(valArray instanceof Array)) {
			throw 'Not array value'
		}
		
		this._appendAnd()
		
		let lc = colName + ' ' + op + ' ( '
		for(let i = 0; i < valArray.length; i++) {
			lc += valArray[i]+','
		}
		
		lc.substr(0,lc.length-1)//去除最后一个逗号
	
		lc += ")"
		
		this.wclause += lc
		return this
	}
	
	_appendAnd() {
		this.wclause += " AND "
		return this
	}
	
	_like(colName,val,lc) {
		this._checkColName(colName)
		this._checkTypeOf(colName,'string',"column name type must be String: "+ colName)
		this._appendAnd()
		this.wclause += colName + lc
		return this
	}
	
	_and(colName,val,op) {
		this._checkColName(colName)
		
		this._appendAnd()
		
		if(this._isString(val)) {
			this.wclause += colName + op +' "' + val + '" '
		} else {
			this.wclause += colName + op + val + ' '
		}
		return this
	}
	
	_checkTypeOf(v,typeName,msg) {
		if(typeof v != typeName) {
			throw msg
		}
	}
	
	_isString(v) {
		return typeof v == 'string'
	}
	
	_checkColName(colName){
		if(!colName) {
			throw "Column name cannot be null"
		}
	}
}

class UpdateBuilder extends Builder{
	
	constructor(table) {
		super(table)
		this.sclause = ''// set clause
	}
	
	sql() {
		let s = 'UPDATE '+ this.table + ' SET '+ this.sclause + ' WHERE 1=1 ' +  this.wclause 
		return s
	}
	
	set(up) {
		let def = enDefs[this.table]
		
		let s = ' '
		for(let k in def) {
			if(typeof up[k] == 'undefined') {
				continue
			}
			
			if(def[k].type=='TEXT') {
				s += k + "='" + up[k] + "',"
			} else {
				s += k + '=' + booleanToInt(up[k]) + ' ,'
			}	
		}
		s = s.substring(0,s.length-1)
		this.sclause = s
		return this
	}
	
}

class SelectBuilder  extends Builder{
	
	constructor(table) {
		super(table)
		this.columns = "*"
		this.lim = -1
		this.offs = null
		this.orderBy = null
	}
	
	sql() {
		let s = 'SELECT '
		if(this.columns == '*') {
			s += '*'
		} else if(this.columns instanceof Array) {
			//去除最后一个逗号
			this.columns.forEach(e=>{
				s = s + e + ','
			})
			s = s.substring(0,s.length-1)
		}else if(typeof this.columns == 'string') {
			s += this.columns
		}
		
		s += ' FROM ' + this.table + ' WHERE 1=1 ' + this.wclause + " "
		
		if(this.orderBy) {
			s += ' ORDER BY ' + this.orderBy + ' '
		}
		
		if(this.lim && this.lim > 0) {
			s += ' LIMIT ' + this.lim
			if(!this.offs) {
				this.offs = 0
			}
			s += ' OFFSET ' + this.offs
		}
		return s
	}
	
	count() {
		let s = 'SELECT count(1) as cnt  FROM ' + this.table + ' WHERE 1=1 ' + this.wclause + " "
		return s
	}

	column(cols) {
		if(cols == null) {
			throw "select column must be String array or *"
		}
		
		if(cols == '*') {
			this.columns = '*'
			return this
		}else if(typeof cols == 'string') {
			this.columns = cols
			return this
		}else {
			for(let i = 0; i < cols.length; i++) {
				if(!cols[i]) {
					throw "column name cannot be null： " + JSON.stringify(cols)
				}
				this._checkTypeOf(cols[i],'string',"column name type must be String: "+ cols[i])
			}
		}
		
		this.columns = cols
		return this
	}
	
	limit(val) {
		this.lim = parseInt(val)
		return this
	}
	
	offset(val) {
		this.offs = parseInt(val)
		return this
	}
	
	order(colName,by) {
		this._checkColName(colName)
		by = by.toUpperCase()
		if(!(by=='DESC' || by=='ASC')) {
			throw 'Invalid order by type'
		}
		this.orderBy = ' ' + colName + ' '+ by + ' '
		return this
	}
	
}

export default {
	dbName: 'jshop',
	version: '0.0.1',

	executeSqlWithoutTx,//非事务执行
	//在事务中执行SQL
	executeSql,//事务执行
	beginTx,//开始事务
	commitTx,//提交事务
	
	tn2en: {}, //从表名取实体名
	en2tn: {},//从实体名取表名
	
	booleanToInt,
	intToBoolean,

	init(opts) {
		opts = opts || {}
		if(opts.dbName) {
			this.dbName = opts.dbName
		} else {
			opts.dbName = this.dbName
		}

		if(opts.version) {
			this.version = opts.version
		}else {
			opts.version = this.version
		}
		console.log("init database")
		return new Promise((reso,reje)=>{
			init(opts)
			.then(rst=>{
				console.log("Init database result: ",rst)
				reso(rst)
			})
			.catch(e=>{
				console.log("Init database error: ",JSON.stringify(e))
				reje(e)
			})
		})
	}
	,getDef(tname) {
		 if(!enDefs[tname]) throw 'Invalid table name: ' + tname
		 return enDefs[tname]
	}
	
	,selectBuilder(table) {
		return new SelectBuilder(table)
	}
	
	,updateBuilder(table) {
		return new UpdateBuilder(table)
	}

	,update(tableName, newValue, condigions) {
		let sql = 'UPDATE ' + tableName + ' Set '
		for (let columnName in newValue) {
			let value = booleanToInt(newValue[columnName])
			if (typeof value == 'string') {
				sql = sql + columnName + "= '" + value + "' "
			} else {
				sql = sql + columnName + '= ' + value
			}
		}

		sql = sql + ' WHERE 1=1 '
		for(let key in condigions) {
			let value = condigions[key]
			if(typeof value == 'string') {
				sql = sql + key + "='" + value + "' AND "
			} else {
				sql = sql + key + '=' + value + ' AND '
			}
		}

		sql = sql.substr(0, sql.length - 4)

		return executeSql(sql)
	}
	
    /**
	 * @param {Object} tableName表名
	 * @param {Object} entity 实体对象
	 */
	,insert(tableName, entity) {
		if(!enDefs[tableName]) {
			throw tableName + ' def not found'
		}
		
		var self = this
		var sql = 'INSERT INTO ' + tableName + ' ( '

		var valuesSql = `values ( `
		var entityDef = enDefs[tableName]

		for (var fname in entityDef) {
			let def = entityDef[fname]
			if (typeof entity[fname] == 'undefined' && typeof def.defaultValue == 'undefined') {
				continue;
			}
			sql += fname + ', '

			var value = entity[fname];
			if (typeof value == 'undefined') {
				value = def.defaultValue;
			}
			value = booleanToInt(value);

			if (def.type == 'TEXT') {
				valuesSql += "'" + value + "', "
			} else {
				valuesSql += value + ', '
			}
		}

		sql = sql.substr(0, sql.length - 2);
		sql = sql + ' ) ';
		valuesSql = valuesSql.substr(0, valuesSql.length - 2);
		valuesSql = valuesSql + ' ) ';
		sql = sql + valuesSql;
		return executeSql(sql);
	}

	,query(sql) {
		return executeSql(sql);
	}
	
	,selectOne(tableName, filter) {
		let sql = `SELECT * FROM ${tableName} WHERE `
		let def = enDefs[tableName]
		for(let key in filter) {
			if(def[key]) {
				
			}
		}
		//typecode=${typecode} and msgId = ${msgId} AND actId=${ownerActId}`
		return executeSql(sql);
	}
	
	,async count(sql) {
		let rst = await executeSql(sql)
		console.log("count:", rst)
		if(rst.code != 0) return rst
		if(rst.data && rst.data.length == 0) {
			rst.data = 0 //0个元素
		} else {
			rst.data = rst.data[0].cnt
		}
		
		rst.count = rst.data
		return rst
	}
	
	//判断某表是否存在：表名、存在回调函数、不存在回调函数
	,async isExitTable(tableName) {
		var self = this;
		var sql = "select * from sqlite_master where type='table' and name = '" + tableName + "'"
		let rst = await executeSqlWithoutTx(sql)
		if (rst.code != 0) return rst

		let result = rst.data
		if (result.length > 0) {
			return {
				code: 0,
				data: true
			}
		} else {
			return {
				code: 0,
				data: false
			}
		}
	}
	
	//删除表数据：表名，删除成功回调函数
	,emptyTable(tableName) {
		return executeSql("delete from " + tableName)
	}
	
	,alterTable(sql) {
		var sql0 = "ALTER TABLE  " + sql;
		return executeSql(sql0);
	}
	
	,addColumn(tableName, columnDef) {
		var sql = tableName + ' ADD COLUMN ' + columnDef;
		return this.alterTable(sql);
	}
	
	//删除表，删除成功回调函数
	,async dropTable(tableName) {
		return executeSqlWithoutTx("drop table IF EXISTS " + tableName)
	}

	,async define(entityName, entityDef) {

		if (!entityName || entityName.trim() == '') {
			throw 'entity name cannot be null';
		}

		let self = this;

		/*
		function entityPrototype() {};
		for (let key in entityDef) {
			let dv = entityDef[key].defaultValue;
			if (dv && dv != 'undefined') {
				entityPrototype.prototype[key] = dv;
			} else {
				entityPrototype.prototype[key] = null;
			}
		}
		*/
	   
		let rst = await self.isExitTable(entityName)
		//console.log(rst)
		
		if (rst.code != 0) {
			return;
		}
		
		if (!rst.data) {
			//表不存在，创建新表
			let createSql = 'CREATE TABLE IF NOT EXISTS ' + entityName + ' ( '
			for (let key in entityDef) {
				let def = entityDef[key];
				let fieldName = def.field || key;
				if (!def.type) {
				throw entityName + ' field type cannot be NULL';
				}
				createSql += fieldName + ' ' + entityDef[key].type
				if (def.primaryKey) {
					createSql += ' PRIMARY KEY ';
					if (def.autoIncrement) {
						createSql += ' AUTOINCREMENT ';
					}
				}

				if (!!def.defaultValue && def.defaultValue != 'undefined') {
					createSql += ' default ' + def.defaultValue;
				}

				createSql += ', '
			}
			createSql = createSql.substr(0, createSql.length - 2);
			createSql += ' )'
			
			enDefs[entityName] = entityDef;
			return await self.executeSqlWithoutTx(createSql);
		} else  {
			let drst = await self.getTablesDef(entityName)
			
			if (drst.code != 0) {
				console.log(drst);
				return 
			}
			
			let tables = drst.data
								
			if (!tables || tables.length <= 0) {
				console.log('no table to get');
				return;
			}
			//console.log(drst)
			
			let table = tables[0];
			//console.log(table);
			let fields = table.fields;
			
			
			for (let key in entityDef) {
				let fieldName = entityDef[key].field || key;
						
				let found = false;
				for (let i = 0; i < fields.length; i++) {
					if (fields[i] == fieldName) {
						found = true;
					}
				}
						
				if (!found) {
					if (!entityDef[key].type) {
						throw entityName + ' field type cannot be NULL';
					}
					let alterSql = fieldName + ' ' + entityDef[key].type;
					if (!!entityDef[key].defaultValue && entityDef[key].defaultValue !=
						'undefined') {
						createSql += ' default ' + entityDef[key].defaultValue;
					}
					self.addColumn(entityName, alterSql, null, null)
				}		
			}
			
			enDefs[entityName] = entityDef;
			
			return {code:0}
			/*
			for (let i = 0; i < fields.length; i++) {
				if (entityDef[fields[i]]) {
					continue
				}
				
				let alterSql = fieldName + ' ' + entityDef[key].type;
				if (!!entityDef[key].defaultValue && entityDef[key].defaultValue !=
					'undefined') {
					createSql += ' default ' + entityDef[key].defaultValue;
				}
				self.dropColumn(entityName, alterSql, null, null)
			}
			*/
		   
		}

	},
	
	 async getTablesDef(entityName) {
		let sql = "SELECT * FROM sqlite_master WHERE type='table' AND name='" + entityName +
			"' AND name NOT LIKE 'sqlite\\_%' escape '\\' AND name NOT LIKE '\\_%' escape '\\'"
		
		let rst = await executeSqlWithoutTx(sql)
		//console.log(rst)
		
		if(rst.code != 0) {
			console.log(rst)
			return rst
		}
		
		let tables = rst.data
		if (!tables.length) {
			//console.log(rst)
			return rst;
		}
	
		//console.log(tables)
		
		let arr = []
		
		for(let i = 0; i < tables.length; i++) {
			let table = tables[i]
			let t = {table:table}
			arr.push(t)
			
			var s = table.sql.split(',');
			s[0] = s[0].replace(new RegExp('create\\s+table\\s+' + table.name + '\\s*\\(', 'i'),
				'');
			t.fields = s.map((i)=>{
				return i.trim().split(/\s/).shift();
			}).filter((i)=>{
				return (i.indexOf(')') === -1)
			})	
		}
		rst.data = arr
		return rst
	}


}
