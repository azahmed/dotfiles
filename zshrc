# file = .zshrc
# get a lot of  Insecure completion-dependent directories detected:...
ZSH_DISABLE_COMPFIX=true

## AM/MOB related stuff
/usr/local/bin/cntlm -f -v -g &>/dev/null &

export GRADLE_HOME=/usr/local/gradle/gradle-4.9


export JAVA_HOME=/Users/P711080/jdk8
export PATH=$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH

export {http,https,ftp}_proxy=http://127.0.0.1:3128
export {HTTP,HTTPS,FTP}_PROXY=http://127.0.0.1:3128

set -o vi
# added by Anaconda3 2019.10 installer
# >>> conda init >>>
# !! Contents within this block are managed by 'conda init' !!
__conda_setup="$(CONDA_REPORT_ERRORS=false '/opt/anaconda3/bin/conda' shell.bash hook 2> /dev/null)"
if [ $? -eq 0 ]; then
    \eval "$__conda_setup"
else
    if [ -f "/opt/anaconda3/etc/profile.d/conda.sh" ]; then
        . "/opt/anaconda3/etc/profile.d/conda.sh"
        CONDA_CHANGEPS1=false conda activate base
    else
        \export PATH="/opt/anaconda3/bin:$PATH"
    fi
fi
unset __conda_setup
# <<< conda init <<<
## AM/MOB End

export ZSH="/Users/P711080/.oh-my-zsh"

ZSH_THEME="agnoster"

plugins=(
 #git
 osx

)

source $ZSH/oh-my-zsh.sh
source /Users/P711080/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh

## aliases
alias uz="source ~/.zshrc"
alias gl="git log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit"
# development
alias MOB="cd ~/code/ATDCIB"
alias AM="cd ~/code/AMACE"
alias MEGA="cd ~/code/ATMEGA"
alias FBI="cd ~/code/ATFCF"

### Functions Start ###
## Docker related
alias dps="docker ps"

function dstop() {
    docker stop "$@"
}

function dinspect() {
    docker inspect "$@"
}

function dbash() {
    docker exec -it $@ /bin/bash
}

function dlogs() {
    docker logs $@
}

function drm() {
    docker rm $@
}

function dkillall() {
  dstopall
  drmall
}

function dstopall() {
    echo "Stopping all containers..."
    docker stop $(docker ps -a -q)
}

function drmall() {
    echo "Removing all containers..."
    docker rm $(docker ps -a -q)
}

function drmiall() {
    docker rmi -f $(docker images -q)
}

function gw() {
    ./gradlew "$@"
}

function whats-on-port() {
    lsof -i TCP:$@
}

function findpid() {
    ps -e | grep "$@"
}
## Git Functions
alias gpull="git pull"
alias gstatus="git status"
alias gpush="git push"
alias gpushf="git push -f"

## UUID
# gen uuid in lowercase
alias uuidgen='uuidgen | tr "[:upper:]" "[:lower:]"'

function git-new-branch() {
    git checkout -b $@
    git push --set-upstream origin $@
}

function git-merge-base-branch() {
    currentBranch=$(git rev-parse --abbrev-ref HEAD)
    git checkout $@
    git pull
    git checkout $currentBranch
    git merge $@
}

function git-new-local-branch() {
    git checkout -b $@
}

function gco() {
    git checkout $@
}

function gclone() {
    git clone $@
}

function gmerge() {
    git merge $@
}

function gca() {
    git commit -am $@
}

## Gradle
function gw() {
    ./gradlew "$@"
}

function mvGradleCache() {
  mv /Users/P711080/.gradle/caches /Users/P711080/.gradle/"$@"
}
### Functions End ###
