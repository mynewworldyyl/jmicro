<template>
    <div class="JCreateTopicView">
        <div style="color:red;">{{msg}}</div>
        <div>
            <label for="topicTitle">{{'topicTitle'|i18n}}</label>
            <Input v-if="topic" id="topicTitle" v-model="topic.title"/>
        </div>

        <!--<div>
            <label for="topicContent">{{'topicContent'|i18n}}</label>&nbsp;&nbsp;&nbsp;
            <Input id="topicContent" style=""  class='textarea' type="textarea" v-model="topic.content"/>
        </div>-->

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
                msg:'',
                isLogin:false,
                editorOption:{

                }
            }
        },

        props:{
            topic:{
                type:Object,
                default:()=>{return {}},
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
                this.$emit("contentChange",this.topic);
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