# Gitlet

## Overview
A version control system that mimics some basic functionalities of Git. This is a project from the esteemed CS61B course at UC Berkley, which I learned online and implemented this project independently.

## What I Learned
- How to select the most suitable data structure, including `LinkedList`, `ArrayList`, `HashMap`, etc, to optimize the time complexity of different operations.
- How to use a graph-like structure to efficiently manage the commit history across all branches in a Git repository.

## Supported Commands
This version control system has the following functionalities:

### init
- #### Usage:
  `java gitlet.Main init`
- #### Description:
  Creates a new Gitlet version-control system in the current directory.

### add
- #### Usage:
  `java gitlet.Main add <file name>`
- #### Description:
  Adds a copy of the file as it currently exists to the staging area.

### commit
- #### Usage:
  `java gitlet.Main commit <message>`
- #### Description:
  Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.

### rm
- #### Usage:
  `java gitlet.Main rm <file name>`
- #### Description:
  Unstages the file if it is currently staged for addition. If the file is tracked in the current commit, stage it for removal and remove the file from the working directory if the user has not already done so.

### log
- #### Usage:
  `java gitlet.Main log`
- #### Description:
  Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, following the first parent commit links, ignoring any second parents found in merge commits.

### global-log
- #### Usage:
  `java gitlet.Main global-log`
- #### Description:
  Like log, except displays information about all commits ever made.

### find
- #### Usage:
  `java gitlet.Main find <commit message>`
- #### Description:
  Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines.

### status
- #### Usage:
  `java gitlet.Main status`
- #### Description:
  Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal. An example of the exact format it should follow is as follows.
  ```
  === Branches ===
  *master
  other-branch
  
  === Staged Files ===
  wug.txt
  wug2.txt 
  
  === Removed Files ===
  goodbye.txt
  
  === Modifications Not Staged For Commit ===
  junk.txt (deleted)
  wug3.txt (modified)
  
  === Untracked Files ===
  random.stuff
  ```

### checkout
- #### Usages:
  1. `java gitlet.Main checkout -- <file name>`
  2. `java gitlet.Main checkout <commit id> -- <file name>`
  3. `java gitlet.Main checkout <branch name>`
- #### Descriptions:
  1. Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.
  2. Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.
  3. Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch are deleted. The staging area is cleared, unless the checked-out branch is the current branch.

### branch
- #### Usage: 
  `java gitlet.Main branch <branch name>`
- #### Description: 
  Creates a new branch with the given name, and points it at the current head commit. This command does NOT immediately switch to the newly created branch (just as in real Git). 


### rm-branch
- #### Usage: 
  `java gitlet.Main rm-branch <branch name>`
- #### Description: 
  Deletes the branch with the given name. This only means to delete the pointer associated with the branch; it does not mean to delete all commits that were created under the branch, or anything like that.

### reset
- #### Usage:
  `java gitlet.Main reset <commit id>`
- #### Description: 
  Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node.

### merge
- #### Usage: 
  `java gitlet.Main merge <branch name>`
- #### Description: 
  Merges files from the given branch into the current branch.