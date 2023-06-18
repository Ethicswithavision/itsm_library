@merge
libraries {
    terraform {
        deploy {
            credentialsId = "tfcb-prod-token"
            // cred_aah_token = "AAH_TOKEN"
            //tf_log = "OFF"
            terraformVersion = "1.3.4"
        }
    }
  	pod_template {
      use_existing_agent {
      	agent_label = "iac-build-agent"
      }
    }
  	// git
    // itsm {
    //     deploy {
    //         credentialsId = "tfcb-prod-token"
    //     }
    // }
}
