Set-Location C:\ICF\Sourcecode\prime-reportstream\prime-reportstream
class Commit {
    [DateTime]$Date
    [string]$BranchName
    [String]$DateRelative
    [String]$Author
    [String]$Email
    [String]$Hash
    [bool]$IsHead
    [String]$Subject
    Commit(){
        $this | Add-Member -Name ShortHash -MemberType ScriptProperty -Value {
            return $this.Hash.Substring(0,8)
        }
        $this | Add-Member -Name BranchLocalName -MemberType ScriptProperty -Value {
            return $this.BranchName.Substring(7) #remove origin/
        }
    }
    [void]Archive() {
        git tag archive/$this.BranchLocalName $this.BranchLocalName
        git push origin archive/$this.BranchLocalName
    }
    [void]DeleteOrigin() {
        # git branch -D $this.BranchName
    }
    [void]DeleteLocal() { 
        # git branch -D $this.BranchLocalName
    }
}

function Get-Branches {
    [CmdletBinding()]
    param (
        [Parameter()][switch]$Merged
    )
    
    if ($merged) { $opt = "--merged" }
    else { $opt = "--no-merged" }

    $sep = ";;"
    [System.Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
    $branchFormat = "%(authordate:iso8601-strict) $sep %(objectname) $sep %(authorname) $sep %(authordate:relative) $sep %(authoremail:trim) $sep %(refname:short) $sep %(contents:subject)"
    $branches = git branch --format="$branchFormat" -r $opt
    $output = @()
    foreach( $branch in $branches ) {
        $out = $branch.Split(" $sep ")
        $commit = [Commit]::new()
        $commit.Date = [DateTime]::Parse($out[0])
        $commit.Hash = $out[1]
        $commit.Author = $out[2]
        $commit.DateRelative = $out[3]
        $commit.Email = $out[4]
        $commit.BranchName = $out[5]
        $commit.Subject = $out[6]
        if ($commit.BranchName -eq "origin/HEAD") {
            $headHash = $commit.Hash
        }

        if ($commit.Hash -eq $headHash) {
            $commit.IsHead = $true
        }

        if ($commit.BranchName -ne "origin/HEAD") {
            $output += $commit
        }
    }
    Write-Output $output
}

Write-Host "Merged"
Get-Branches -Merged | Format-Table
Write-Host "No merged"
Get-Branches | Format-Table