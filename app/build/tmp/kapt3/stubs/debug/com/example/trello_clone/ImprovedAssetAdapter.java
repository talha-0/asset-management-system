package com.example.trello_clone;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0015B\u0013\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0002J\b\u0010\u000b\u001a\u00020\fH\u0016J\u001c\u0010\r\u001a\u00020\u000e2\n\u0010\u000f\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0010\u001a\u00020\fH\u0016J\u001c\u0010\u0011\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\fH\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/example/trello_clone/ImprovedAssetAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/trello_clone/ImprovedAssetAdapter$AssetViewHolder;", "assets", "", "Lcom/example/trello_clone/AssetItem;", "(Ljava/util/List;)V", "formatDate", "", "timestamp", "", "getItemCount", "", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "AssetViewHolder", "app_debug"})
public final class ImprovedAssetAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.trello_clone.ImprovedAssetAdapter.AssetViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.trello_clone.AssetItem> assets = null;
    
    public ImprovedAssetAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.trello_clone.AssetItem> assets) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.trello_clone.ImprovedAssetAdapter.AssetViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.trello_clone.ImprovedAssetAdapter.AssetViewHolder holder, int position) {
    }
    
    private final java.lang.String formatDate(long timestamp) {
        return null;
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0011\u0010\u000f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006\u0012"}, d2 = {"Lcom/example/trello_clone/ImprovedAssetAdapter$AssetViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/example/trello_clone/ImprovedAssetAdapter;Landroid/view/View;)V", "assetCard", "Landroidx/cardview/widget/CardView;", "getAssetCard", "()Landroidx/cardview/widget/CardView;", "assetDetails", "Landroid/widget/TextView;", "getAssetDetails", "()Landroid/widget/TextView;", "assetName", "getAssetName", "statusIndicator", "getStatusIndicator", "()Landroid/view/View;", "app_debug"})
    public final class AssetViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView assetName = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView assetDetails = null;
        @org.jetbrains.annotations.NotNull()
        private final android.view.View statusIndicator = null;
        @org.jetbrains.annotations.NotNull()
        private final androidx.cardview.widget.CardView assetCard = null;
        
        public AssetViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View itemView) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getAssetName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getAssetDetails() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.view.View getStatusIndicator() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.cardview.widget.CardView getAssetCard() {
            return null;
        }
    }
}