package cn.jmicro.ext.bbs.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.ext.bbs.entities.NoteJRso;
import cn.jmicro.ext.bbs.entities.TopicJRso;
import cn.jmicro.ext.bbs.entities.TopicVoJRso;

@AsyncClientProxy
public interface IBbsServiceJMSrv {

	public static final String T_TOPIC_NAME="t_topic";
	
	public static final String T_NOTE_NAME="t_note";
	
	public static final String T_TOPIC_TYPE="t_note";
	
	public static final String T_USER = "t_user";
	
	RespJRso<Boolean> createTopic(TopicJRso topic);
	
	RespJRso<Boolean> updateTopic(TopicJRso topic);
	
	RespJRso<Boolean> deleteTopic(Long topicId);
	
	RespJRso<List<TopicJRso>> topicList(Map<String,String> qry,int pageSize,int offset);
	
	RespJRso<Long> countTopic(Map<String,String> qry);
	
	RespJRso<TopicVoJRso> getTopic(long topicId);
	
	RespJRso<List<NoteJRso>> topicNoteList(long topicId, int pageSize, int curPage);
	
	RespJRso<NoteJRso> createNote(NoteJRso note);
	
	RespJRso<Boolean> updateNote(NoteJRso note);
	
	RespJRso<Boolean> deleteNote(Long noteId);
}
