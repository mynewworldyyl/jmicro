<template>
    <div class="JShell" id="terminal"></div>
</template>

<script>

    import "xterm/css/xterm.css";
    import { Terminal } from 'xterm'
    import { FitAddon } from 'xterm-addon-fit';


    export default {
        name: 'JShell',
        data () {
            return {
                term: null,
                terminalSocket: null

            }
        },
        methods: {
            runRealTerminal () {
                console.log('webSocket is finished')
            },
            errorRealTerminal () {
                console.log('error')
            },
            closeRealTerminal () {
                console.log('close')
            }
        },

        mounted () {
            let self = this;
            let terminalContainer = document.getElementById('terminal')
            this.term = new Terminal(
                {
                    cols: 92,
                    rows: 30,
                    cursorBlink: true, // 光标闪烁
                    cursorStyle: "underline", // 光标样式  null | 'block' | 'underline' | 'bar'
                    scrollback: 800, //回滚
                    tabStopWidth: 8, //制表宽度
                    screenKeys: true//
                }
            );

            // 换行并输入起始符“$”
            this.term.prompt = () => {
                self.term.write("\r\n$ ");
                //self.term.write(" ");
            };

            this.term.open(terminalContainer, true);
            const fitAddon = new FitAddon();
            this.term.loadAddon(fitAddon);
            // Make the terminal's size and geometry fit the size of #terminal-container
            //fitAddon.fit();

            this.term.writeln("JMicro service terminal $ ");
            this.term.prompt();

            this.term.onKey(keyEvt => {
               // console.log(keyEvt);
                if(keyEvt.domEvent.key == 'v' && keyEvt.domEvent.ctrlKey) {
                    self.term.write(self.copy);
                }else if(keyEvt.domEvent.keyCode == 13) {
                    self.term.prompt();
                }
            })

            this.term.onData( data => {
                self.term.write(data)
            })

            this.term.onSelectionChange(()=>{
                if (self.term.hasSelection()) {
                    self.copy = self.term.getSelection();
                }
            })

        },

        beforeDestroy () {

        }
    }
</script>

<style>
    .JShell{
        overflow-y:hidden;
        overflow-x:hidden;
    }

</style>