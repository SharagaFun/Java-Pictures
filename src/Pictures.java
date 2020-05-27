import com.drew.imaging.*;
import com.drew.metadata.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Pictures extends JFrame
{
    private static final long serialVersionUID = 1L;
    private  JButton      btnFileOpen;
    private  JFileChooser fileChooser;
    private JTable table;
    int reference = 0;
    ArrayList<HashMap<String, Object>> pics;
    LinkedHashSet<String> tableColumns;

    private void updateTable ()
    {
        if (pics.size() == 0)
            return;
        ArrayList<ArrayList<Object>> tableRows = new ArrayList<>();
        ArrayList<ImageIcon> icons = new ArrayList<>();
        PictureService pserv = new PictureService();
        for (HashMap<String, Object> pic : pics)
        {
            try {
                pic.put("Similarity", pserv.getSimilarityPercent((String) pic.get("Path"), (String) pics.get(reference).get("Path")));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening image");
                pic.put("Similarity", "n/a");
            }
            tableRows.add(new ArrayList<>());
            for (String column : tableColumns)
            {
                tableRows.get(tableRows.size() - 1).add(pic.get(column));
            }
            icons.add (new ImageIcon(new ImageIcon((String)pic.get("Path")).getImage().getScaledInstance(75, 75, Image.SCALE_SMOOTH)));
        }
        DefaultTableModel model = new DefaultTableModel(tableColumns.toArray(), 0){
            @Override
            public Class<?> getColumnClass(int column) {
                switch(column) {
                    case 0: return Boolean.class;
                    case 1:
                        return ImageIcon.class;
                    default: return Object.class;
                }
            } };
        for (ArrayList<Object> row: tableRows)
        {
            model.addRow(row.toArray());
        }
        table.setModel(model);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        for (int i=0; i<table.getRowCount(); i++) {
            table.setValueAt(icons.get(i), i, 1);
        }
        sorter.setSortKeys(sortKeys);
        table.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (column == 0) {
                TableModel modell = (TableModel) e.getSource();
                Boolean checked = (Boolean) modell.getValueAt(row, column);
                int id = (int) modell.getValueAt(row, 2) - 1;
                if (checked && id != reference)
                {
                    pics.get(reference).put("Is reference", false);
                    reference = id;
                    pics.get(id).put("Is reference", true);
                }
                updateTable();
            }
        });


    }
    public Pictures() {
        super("Pictures");
        pics = new ArrayList<>();
        tableColumns = new LinkedHashSet<>();
        tableColumns.add("Is reference");
        tableColumns.add("Preview");
        tableColumns.add("id");
        tableColumns.add("Similarity");
        tableColumns.add("File Name");
        tableColumns.add("Path");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        btnFileOpen = new JButton("Open file");
        fileChooser = new JFileChooser();
        addFileChooserListeners();
        JPanel contents = new JPanel();
        setContentPane(contents);
        table = new JTable();
        table.setDefaultEditor(Object.class, null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(100);
        updateTable();
        contents.add(btnFileOpen);
        JScrollPane scrollpane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollpane.setPreferredSize(new Dimension(1000, 500));
        contents.add(scrollpane);
        setSize(1020, 570);
        setVisible(true);

    }

    private void addFileChooserListeners()
    {
        btnFileOpen.addActionListener(e -> {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                   "JPG and PNG", "jpg", "png");
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(Pictures.this);

            if (result == JFileChooser.APPROVE_OPTION )
            {
                for ( File f : fileChooser.getSelectedFiles())
                {
                    pics.add(new HashMap<>());
                    pics.get(pics.size()-1).put("Path", f.getPath());
                    pics.get(pics.size()-1).put("Is reference", pics.size()==1);
                    pics.get(pics.size()-1).put("id", pics.size());
                    Metadata metadata = null;
                    try {
                        metadata = ImageMetadataReader.readMetadata(f);
                    } catch (ImageProcessingException ex) {
                        JOptionPane.showMessageDialog(this, "Error processing image");
                        pics.remove(pics.size()-1);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error opening image");
                        pics.remove(pics.size()-1);
                    }
                    for (Directory directory : metadata.getDirectories()) {
                        for (Tag tag : directory.getTags()) {
                            pics.get(pics.size()-1).put(tag.getTagName(),tag.getDescription());
                            tableColumns.add(tag.getTagName());
                        }
                    }
                }
                updateTable();
            }
        });
    }

    public static void main(String[] args)
    {
        new Pictures();
    }
}
