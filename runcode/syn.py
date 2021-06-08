import subprocess
import sys
import os


class RunCCode:
    
    def __init__(self, code, ins, outs):
        self.code = code
        self.compiler = './strix'
        self.ins= '--ins=' + ins
        self.outs='--outs=' + outs
    	
    def _compile_c_code(self, code, ins, outs):
        cmd = [self.compiler, '-f', code, self.ins, self.outs, '--k', '--dot', '--minimize']
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        result = p.wait()
        a, b = p.communicate()
        self.stdout, self.stderr = a.decode("utf-8"), b.decode("utf-8")
        return self.stdout, self.stderr

  
    
    def run_c_code(self, code, ins, outs):
        if not code:
            code = self.code
        if not ins:
            ins = self.ins
        if not outs:
            outs = self.outs
        res, err = self._compile_c_code(code, ins, outs) 
        return err, res



