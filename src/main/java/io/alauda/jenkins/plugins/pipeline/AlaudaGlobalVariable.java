package io.alauda.jenkins.plugins.pipeline;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;

@Extension
public class AlaudaGlobalVariable extends GlobalVariable {

    @Nonnull
    @Override
    public String getName(){
        return "alauda";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript script) throws Exception{
        Binding binding = script.getBinding();
        script.println();
        Object alauda;

        if(binding.hasVariable(getName())){
            alauda = binding.getVariable(getName());
        }else{
            alauda = script.getClass().getClassLoader()
                    .loadClass("io.alauda.jenkins.plugins.pipeline.AlaudaDSL")
                    .getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), alauda);
        }
        return alauda;
    }

}
