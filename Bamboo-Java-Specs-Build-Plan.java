import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.AtlassianModule;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.BambooOid;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup;
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.repository.VcsChangeDetection;
import com.atlassian.bamboo.specs.api.builders.task.AnyTask;
import com.atlassian.bamboo.specs.builders.repository.git.UserPasswordAuthentication;
import com.atlassian.bamboo.specs.builders.repository.github.GitHubRepository;
import com.atlassian.bamboo.specs.builders.repository.viewer.GitHubRepositoryViewer;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.trigger.RepositoryPollingTrigger;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;

@BambooSpec
public class PlanSpec {
    
    public Plan plan() {
        final Plan plan = new Plan(new Project()
                .oid(new BambooOid("1wunpgyie0r9e"))
                .key(new BambooKey("SAM"))
                .name("sample-app")
                .description("Sample CDK App"),
            "main-plan",
            new BambooKey("MAIN"))
            .oid(new BambooOid("1wue09d566yv7"))
            .description("Main plan")
            .pluginConfigurations(new ConcurrentBuilds())
            .stages(new Stage("Default Stage")
                    .jobs(new Job("Default Job",
                            new BambooKey("JOB1"))
                            .artifacts(new Artifact()
                                    .name("cdk.out")
                                    .copyPattern("**")
                                    .location("cdk.out")
                                    .shared(true)
                                    .required(true))
                            .tasks(new VcsCheckoutTask()
                                    .description("Checkout Default Repository")
                                    .checkoutItems(new CheckoutItem().defaultRepository()),
                                new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.identity-federation-for-aws-bamboo:aws.sts.credentialsvariables"))
                                    .description("Setting AWS credentials")
                                    .configuration(new MapBuilder()
                                            .put("pluginConfigVersionOnSave", "1")
                                            .put("addCallerIdentityVariables", "")
                                            .put("awsIamRoleAgentsArn", "")
                                            .put("pluginVersionOnSave", "2.13.1")
                                            .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                            .put("awsConnectorIdVariable", "")
                                            .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                            .build()),
                                new ScriptTask()
                                    .description("cdk synth")
                                    .inlineBody("npm install -g typescript\nnpm install typescript\ncdk synth"))))
            .planRepositories(new GitHubRepository()
                    .name("bamboo-sample-cdk-app")
                    .oid(new BambooOid("1wui5x6veovly"))
                    .repositoryViewer(new GitHubRepositoryViewer())
                    .repository("mosmabro/bamboo-sample-cdk-app")
                    .branch("master")
                    .authentication(new UserPasswordAuthentication("mosmabro@xxxxx")
                            .password("xxxxxxxxxx"))
                    .changeDetection(new VcsChangeDetection()))
            
            .triggers(new RepositoryPollingTrigger())
            .planBranchManagement(new PlanBranchManagement()
                    .delete(new BranchCleanup())
                    .notificationForCommitters());
        return plan;
    }
    
    public PlanPermissions planPermission() {
        final PlanPermissions planPermission = new PlanPermissions(new PlanIdentifier("SAM", "MAIN"))
            .permissions(new Permissions()
                    .userPermissions("mosmabro", PermissionType.EDIT, PermissionType.VIEW, PermissionType.ADMIN, PermissionType.CLONE, PermissionType.BUILD));
        return planPermission;
    }
    
    public static void main(String... argv) {
        //By default credentials are read from the '.credentials' file.
        BambooServer bambooServer = new BambooServer("http://192.168.1.12:8085");
        final PlanSpec planSpec = new PlanSpec();
        
        final Plan plan = planSpec.plan();
        bambooServer.publish(plan);
        
        final PlanPermissions planPermission = planSpec.planPermission();
        bambooServer.publish(planPermission);
    }
}
