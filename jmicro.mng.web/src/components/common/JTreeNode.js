
export default class TreeNode {

    constructor(id = '', title = '', children = [], parent = null, val = null,label) {
        this.id = id;
        this.title = title;
        this.children = children;
        this.val = val;
        this.parent = parent;
        this.type = '';
        this.label = label;
        this.group = '';
    }

    addChild(node) {
        this.children.push(node);
    }

    removeChild(node) {
        let idx = this.indexOfChild(node);
        if (idx >= 0) {
            this.children.splice(idx, 1);
        }
    }

    indexOfChild(node) {
        if (!this.children || this.children.length == 0) {
            return -1;
        }
        for (let i = 0; i < this.children.length; i++) {
            if (this.children[i] == node) {
                return i;
            }
        }
        return -1;
    }

}