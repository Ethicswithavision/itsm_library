void call() {
    // Get TFCB Credentials from build config
    String credentialsId = config.deploy.credentialsId
	println ("Check status")

    // Checkout target repository
    def checkout = checkout(scm)
    // environment {
    //     AAH_URL = "https://stage-ansiblepah.sunlifecorp.com/api/galaxy/content/published/"
    //     AAH_TOKEN = "AAH_TOKEN"
    // }

    stage("CI Creation") {

        steps{

        // Collect credentials from Jenkins Credentials
        withCredentials([string(credentialsId: "${credentialsId}", variable: 'TF_TOKEN_app_terraform_io')]) {

        // Set any environment variables

        // Had to download terraform as the agent does not have the correct version to use the TFCB cloud provider block
        script {

            def statefile=sh(script:'cd terraform;terraform init; terraform state pull > terraform.json',returnStdout:true).trim()
            //println (statefile)
            sh '''
            ls
            AAH_URL="https://stage-ansiblepah.sunlifecorp.com/api/galaxy/content/published/"
            AAH_TOKEN="90d53fe5880984ebb7727bc86cc365f1113a0bef"
            ansible --version
            ls
            out=`pwd`
            #mkdir -p plugins/files/
            ls
            ansible-galaxy collection install servicenow.itsm
            #export ANSIBLE_CFG=$wrkdir/ansible.cfg
            #ansible-galaxy collection install enterprisedevops.servicenowci_v6:1.0.0
            ansible-galaxy collection install --token $AAH_TOKEN --server $AAH_URL enterprisedevops.servicenowci_v6:1.0.0 --ignore-certs -vvv
            #ansible-galaxy collection build --force
            cd $out
            #ansible-galaxy collection install enterprisedevops-servicenowci_v3-1.0.0.tar.gz
            cp -r ~/.ansible/collections/ansible_collections/enterprisedevops/servicenowci_v6/plugins $out
            wrkdir=$out/plugins/files
            cd $wrkdir
            cp -r ../../terraform/terraform.json terraform.json
            ls
            state_content=`cat terraform.json`;
            echo $state_content
            chmod +x get_vars.sh
            bash get_vars.sh
            ls
            state_content_1=`cat split_sub_0.yml`;
            echo $state_content_1
            echo "Extraction of tags from statefile from resource block" 
            pwd
            ls 
            echo "List of extracted var files"
            ls split_*.yml
            chmod +x exec_playbook_ci_create.sh 
            echo "CI updation based on the var files"
            bash exec_playbook_ci_create.sh 
            echo "End of CI creation"
            '''
        }
      }
    }

    }
    
  
        stage('CI Updation') {
        //agent { label 'terraform-1.1.5' }
        withCredentials([string(credentialsId: "${credentialsId}", variable: 'TF_TOKEN_app_terraform_io')]) {      
            script{ 
                def statefile=sh(script:'cd terraform;terraform init; terraform state pull > terraform.json',returnStdout:true).trim()
                echo "Extract the variables from the terraform statefile";
                sh'''
                AAH_URL="https://stage-ansiblepah.sunlifecorp.com/api/galaxy/content/published/"
                AAH_TOKEN="90d53fe5880984ebb7727bc86cc365f1113a0bef"
                out=`pwd`
                ansible-galaxy collection install servicenow.itsm
                #ansible-galaxy collection build --force 
                #ansible-galaxy collection install enterprisedevops-servicenowci_v6:1.0.0.tar.gz
                ansible-galaxy collection install --token $AAH_TOKEN --server $AAH_URL enterprisedevops.servicenowci_v6:1.0.0 --ignore-certs -vvv
                cp -r ~/.ansible/collections/ansible_collections/enterprisedevops/servicenowci_v6/plugins $out
                wrkdir=$out/plugins/files
                cd $wrkdir
                cp -r ../../terraform/terraform.json terraform.json
                chmod +x get_vars.sh
                bash get_vars.sh
                echo "Extraction of tags from statefile from resource block" 
                pwd
                ls 
                echo "List of extracted var files"
                ls split_*.yml
                chmod +x execute_playbook.sh 
                echo "CI updation based on the var files"
                bash execute_playbook.sh 
                echo "Check"
                echo "End of CI Updation"
                '''
                
            }

                
        }

    }
        
            
}
