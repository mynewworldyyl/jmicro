package cn.jmicro.ext.bbs.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.ext.bbs.entities.Note;
import cn.jmicro.ext.bbs.entities.Topic;
import cn.jmicro.ext.bbs.entities.TopicVo;

@AsyncClientProxy
public interface IBbsService {

	public static final String T_TOPIC_NAME="t_topic";
	
	public static final String T_NOTE_NAME="t_note";
	
	public static final String T_TOPIC_TYPE="t_note";
	
	public static final String T_USER = "t_user";
	
	Resp<Boolean> createTopic(Topic topic);
	
	Resp<Boolean> updateTopic(Topic topic);
	
	Resp<Boolean> deleteTopic(Long topicId);
	
	Resp<List<Topic>> topicList(Map<String,String> qry,int pageSize,int offset);
	
	Resp<Long> countTopic(Map<String,String> qry);
	
	Resp<TopicVo> getTopic(long topicId);
	
	Resp<List<Note>> topicNoteList(long topicId, int pageSize, int curPage);
	
	Resp<Note> createNote(Note note);
	
	Resp<Boolean> updateNote(Note note);
	
	Resp<Boolean> deleteNote(Long noteId);
}
