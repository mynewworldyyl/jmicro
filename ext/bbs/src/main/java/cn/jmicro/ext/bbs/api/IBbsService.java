package cn.jmicro.ext.bbs.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.ext.bbs.entities.Note;
import cn.jmicro.ext.bbs.entities.NoteVo;
import cn.jmicro.ext.bbs.entities.Topic;
import cn.jmicro.ext.bbs.entities.TopicVo;

@AsyncClientProxy
public interface IBbsService {

	public static final String T_TOPIC_NAME="t_topic";
	
	public static final String T_NOTE_NAME="t_note";
	
	public static final String T_TOPIC_TYPE="t_note";
	
	Resp<Boolean> createTopic(Topic topic);
	
	Resp<List<Topic>> topicList(Map<String,String> qry,int pageSize,int offset);
	
	Resp<Long> countTopic(Map<String,String> qry);
	
	Resp<TopicVo> getTopic(long topicId);
	
	Resp<List<NoteVo>> topicNoteList(long topicId, int pageSize, int curPage);
	
	Resp<Boolean> createNote(Note note);
	
}
