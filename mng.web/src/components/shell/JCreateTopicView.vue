<template>
    <div v-if="topic" class="JCreateTopicView">
        <div style="color:red;">{{msg}}</div>

        <div>
            <label for="topicTitle">{{'topicTitle'|i18n}}</label>
            <Input v-if="topic" id="topicTitle" v-model="topic.title"/>
        </div>

        <div>
            <Label for="topicType">Type</Label>
            <Select id="topicType" v-model="topic.topicType">
                <Option v-for="v in topicTypes" :value="v" :key="'type_'+v">{{v | i18n}}</Option>
            </Select>
        </div>
        <quill-editor v-if="topic"
                      ref="myQuillEditor"
                      v-model="topic.content"
                      :options="editorOption"
                      @blur="onEditorBlur($event)"
                      @focus="onEditorFocus($event)"
                      @ready="onEditorReady($event)"
        />

    </div>
</template>

<script>

    import { quillEditor } from "vue-quill-editor";

    import 'quill/dist/quill.core.css'
    import 'quill/dist/quill.snow.css'
    import 'quill/dist/quill.bubble.css'

    const cid = 'JCreateTopicView';

   /* import Quill from 'quill'
    import yourQuillModule from '../yourModulePath/yourQuillModule.js'
    Quill.register('modules/yourQuillModule', yourQuillModule)*/

    export default {
        name: cid,
        data() {
            return {
                topicTypes:["Pubsub","MicroService","Monitor","ConfigMng","ServiceMng","NewFeature","Other"],
                msg:'',
                isLogin:false,
                editorOption:{

                }
            }
        },

        props:{
            topic:{
                type:Object,
            }
        },

        components: {
            //quillEditor : () => import('vue-quill-editor'),
            quillEditor,
        },

        methods: {

            onEditorFocus() {

            },

            onEditorBlur() {

            },

            onEditorReady() {
                this.$bus.$emit("contentChange",this.topic);
            },

        },

        mounted () {
            //let self = this;

        },

        beforeDestroy() {

        },

    }
</script>

<style>

    .JCreateTopicView{

    }

</style>