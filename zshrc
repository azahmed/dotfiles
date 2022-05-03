# If you come from bash you might have to change your $PATH.
# export PATH=$HOME/bin:/usr/local/bin:$PATH

# Path to your oh-my-zsh installation.
export ZSH="/Users/ahmeda/.oh-my-zsh"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home
export AWS_REGION="ap-southeast-2"
export AWS_PROFILE="streamotion-platform-nonprod"


# Set name of the theme to load --- if set to "random", it will
# load a random theme each time oh-my-zsh is loaded, in which case,
# to know which specific one was loaded, run: echo $RANDOM_THEME
# See https://github.com/ohmyzsh/ohmyzsh/wiki/Themes
ZSH_THEME="agnoster"

# Set list of themes to pick from when loading at random
# Setting this variable when ZSH_THEME=random will cause zsh to load
# a theme from this variable instead of looking in $ZSH/themes/
# If set to an empty array, this variable will have no effect.
# ZSH_THEME_RANDOM_CANDIDATES=( "robbyrussell" "agnoster" )

# Uncomment the following line to use case-sensitive completion.
# CASE_SENSITIVE="true"

# Uncomment the following line to use hyphen-insensitive completion.
# Case-sensitive completion must be off. _ and - will be interchangeable.
# HYPHEN_INSENSITIVE="true"

# Uncomment one of the following lines to change the auto-update behavior
# zstyle ':omz:update' mode disabled  # disable automatic updates
# zstyle ':omz:update' mode auto      # update automatically without asking
# zstyle ':omz:update' mode reminder  # just remind me to update when it's time

# Uncomment the following line to change how often to auto-update (in days).
# zstyle ':omz:update' frequency 13

# Uncomment the following line if pasting URLs and other text is messed up.
# DISABLE_MAGIC_FUNCTIONS="true"

# Uncomment the following line to disable colors in ls.
# DISABLE_LS_COLORS="true"

# Uncomment the following line to disable auto-setting terminal title.
# DISABLE_AUTO_TITLE="true"

# Uncomment the following line to enable command auto-correction.
# ENABLE_CORRECTION="true"

# Uncomment the following line to display red dots whilst waiting for completion.
# You can also set it to another string to have that shown instead of the default red dots.
# e.g. COMPLETION_WAITING_DOTS="%F{yellow}waiting...%f"
# Caution: this setting can cause issues with multiline prompts in zsh < 5.7.1 (see #5765)
# COMPLETION_WAITING_DOTS="true"

# Uncomment the following line if you want to disable marking untracked files
# under VCS as dirty. This makes repository status check for large repositories
# much, much faster.
# DISABLE_UNTRACKED_FILES_DIRTY="true"

# Uncomment the following line if you want to change the command execution time
# stamp shown in the history command output.
# You can set one of the optional three formats:
# "mm/dd/yyyy"|"dd.mm.yyyy"|"yyyy-mm-dd"
# or set a custom format using the strftime function format specifications,
# see 'man strftime' for details.
# HIST_STAMPS="mm/dd/yyyy"

# Would you like to use another custom folder than $ZSH/custom?
# ZSH_CUSTOM=/path/to/new-custom-folder

# Which plugins would you like to load?
# Standard plugins can be found in $ZSH/plugins/
# Custom plugins may be added to $ZSH_CUSTOM/plugins/
# Example format: plugins=(rails git textmate ruby lighthouse)
# Add wisely, as too many plugins slow down shell startup.
plugins=(
 #git
 macos

)

source $ZSH/oh-my-zsh.sh
source /Users/ahmeda/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh

# User configuration

# export MANPATH="/usr/local/man:$MANPATH"

# You may need to manually set your language environment
# export LANG=en_US.UTF-8

# Preferred editor for local and remote sessions
# if [[ -n $SSH_CONNECTION ]]; then
#   export EDITOR='vim'
# else
#   export EDITOR='mvim'
# fi

# Compilation flags
# export ARCHFLAGS="-arch x86_64"

# Set personal aliases, overriding those provided by oh-my-zsh libs,
# plugins, and themes. Aliases can be placed here, though oh-my-zsh
# users are encouraged to define aliases within the ZSH_CUSTOM folder.
# For a full list of active aliases, run `alias`.
#
# Example aliases
# alias zshconfig="mate ~/.zshrc"
# alias ohmyzsh="mate ~/.oh-my-zsh"

## aliases
alias uz="source ~/.zshrc"
alias gl="git log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit"
# development
alias C="~/code/"
alias or="docker run -it --rm -v ~/.aws:/root/.aws kayosportsau/ubuntu-okta:1.0.1 -c 'oktashell.sh -u asrarz.ahmed@foxtel.com.au -p okta'"

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
alias gp='git pull'
alias gpp='git pull --prune'
alias gstatus="git status"
alias gpush="git push"
alias gpushf="git push -f"

  ## UUID
# gen uuid in lowercase
alias uuidgen='uuidgen | tr "[:upper:]" "[:lower:]"'


function oa() {
    open -a "Atom" $@
    # git push --set-upstream origin $@
}


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

function git-clean() {
              echo "Removing all local branches except master"
              git branch | grep -v "master" | xargs git branch -D
}

## Gradle
function gw() {
    ./gradlew "$@"
}

function mvGradleCache() {
  mv /Users/P711080/.gradle/caches /Users/P711080/.gradle/"$@"
}

# Mvn commands
alias mci='mvn clean install'
alias mcif='mvn clean install -U'
alias mcheck='mvn checkstyle:checkstyle'
alias mcomp='mvn compile'
### Functions End ###
