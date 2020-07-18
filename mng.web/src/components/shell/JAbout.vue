<template>
    <div class="JAbout" id="JAbout">
        <h3>为什么写JMicro</h3>
        <p>
        印象中初次接触微服务大概是2011年，那会做Eclpise插件开发，网上查看好多关于OSGI的技术文章，发现Spring新出了一个叫Spring-boot的框架，那会没太上心，只是了解了点皮毛，工作又太忙，之后就没下文了。
        </p>
        <p>
        直到大概2015年的某天，碰到一个小项目，没什么难度，都用老套路去玩，没什么意思，得玩点新东西才行，也不枉一翻付出，于是选择用GO语言实现，选择GO主要是想体验一下GO，看是不是真如传说中的那样无敌。经过一翻折腾，最终确定GOGIN+GOMICRO实现。是的，从那会开始，通过学心和使用GOMICRO，从此迷上微服务。后来因为工作需要，再没什么机会在项目中接触GO。
        </p>
        <p>
        后面也曾试图去用Dubbo和Spring-Cloud做项目，但也止于浅尝则止，没能深入。一方面项目时间太紧，折腾不起。另一方面，也是最重要的，项目组成员根本不愿意去学新东西，在很多成员心中，微服务，Spring-Cloud太深奥，玩不起，没时间，至于Dubbo，写个服务，做个RPC也是从百度复制下来的，跑起来就完事了，没人关心个中的原理，经常碰到问题就抓狂！
        </p>
        <p>
        从那时候起，就一直琢磨着能不能用Java写一个像GoMicro一样简单的微服务框架，这个框架要确保足够简单，入门和使用成本要底，就像写HelloWord一样，但微服务的基本功能要全。因此项目依赖不能多，打出来的运行包也要小，能不用的第三方包坚决不用，最好使用JDK库就行！
        </p>
    </div>
</template>

<script>

    export default {
        name: 'JAbout',
        data () {
            return {

            }
        },
        methods: {

        },

        mounted () {
        },

        beforeDestroy () {

        }
    }
</script>

<style>
    .JAbout{

    }

</style>